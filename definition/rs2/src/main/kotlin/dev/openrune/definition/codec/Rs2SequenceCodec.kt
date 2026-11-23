package dev.openrune.definition.codec

import dev.openrune.definition.revisionIsOrAfter
import dev.openrune.definition.writeParameterBody
import dev.openrune.definition.type.Rs2SequenceSound
import dev.openrune.definition.type.Rs2SequenceType
import dev.openrune.definition.util.readSmart
import dev.openrune.definition.util.writeSmart
import io.netty.buffer.ByteBuf

class Rs2SequenceCodec(private val revision: Int) : Rs2OrderedCodec<Rs2SequenceType> {

    private val parallelFrames get() = revisionIsOrAfter(revision, 457)
    private val wideWalkMerge get() = revisionIsOrAfter(revision, 751)
    private val countedSounds get() = revisionIsOrAfter(revision, 499)

    override fun Rs2SequenceType.read(opcode: Int, buffer: ByteBuf) {
        when (opcode) {
            1 -> if (!parallelFrames) {
                val count = buffer.readUnsignedByte().toInt()
                legacyFrames = true
                val frameList = ArrayList<Int>(count)
                val iFrameList = ArrayList<Int>(count)
                val delayList = ArrayList<Int>(count)
                repeat(count) {
                    frameList += buffer.readUnsignedShort()
                    iFrameList += buffer.readUnsignedShort()
                    delayList += buffer.readUnsignedShort()
                }
                frames = frameList
                legacyIFrames = iFrameList
                delays = delayList
            } else {
                val count = buffer.readUnsignedShort()
                val delayList = MutableList(count) { buffer.readUnsignedShort() }
                val frame = MutableList(count) { buffer.readUnsignedShort() }
                val frameSet = MutableList(count) { buffer.readUnsignedShort() }
                delays = delayList
                frames = MutableList(count) { (frameSet[it] shl 16) or frame[it] }
            }

            2 -> loops = buffer.readUnsignedShort()
            3 -> {
                val count = if (wideWalkMerge) buffer.readSmart() else buffer.readUnsignedByte().toInt()
                walkMerge = MutableList(count) {
                    if (wideWalkMerge) buffer.readSmart() else buffer.readUnsignedByte().toInt()
                }
            }

            4 -> if (revisionIsOrAfter(revision, 204)) stretches = true else unknown4 = buffer.readUnsignedShort()
            5 -> priority = buffer.readUnsignedByte().toInt()
            6 -> replaceHeldLeft = buffer.readUnsignedShort()
            7 -> replaceHeldRight = buffer.readUnsignedShort()
            8 -> maxLoops = buffer.readUnsignedByte().toInt()
            9 -> preAnimMove = buffer.readUnsignedByte().toInt()
            10 -> postAnimMove = buffer.readUnsignedByte().toInt()
            11 -> duplicateBehaviour = buffer.readUnsignedByte().toInt()
            12, 112 -> {
                val count = if (opcode == 112) buffer.readUnsignedShort() else buffer.readUnsignedByte().toInt()
                iFramesWideCount = opcode == 112
                val frame = MutableList(count) { buffer.readUnsignedShort() }
                val frameSet = MutableList(count) { buffer.readUnsignedShort() }
                iFrames = MutableList(count) { (frameSet[it] shl 16) or frame[it] }
            }

            13 -> if (!countedSounds) {
                val count = buffer.readUnsignedByte().toInt()
                sounds = MutableList(count) {
                    val value = buffer.readUnsignedMedium()
                    Rs2SequenceSound(packed = value.takeIf { it != 0 })
                }
            } else {
                val count = buffer.readUnsignedShort()
                sounds = MutableList(count) {
                    val entryCount = buffer.readUnsignedByte().toInt()
                    if (entryCount == 0) {
                        Rs2SequenceSound()
                    } else {
                        val packed = buffer.readUnsignedMedium()
                        val extra = MutableList(entryCount - 1) { buffer.readUnsignedShort() }
                        Rs2SequenceSound(packed, extra)
                    }
                }
            }

            14 -> unknown14 = true
            15 -> unknown15 = true
            16 -> unknown16 = true
            17 -> unknown17 = buffer.readUnsignedByte().toInt()
            18 -> soundVorbis = true
            19, 119 -> {
                soundVolumeWide = opcode == 119
                soundVolume = if (opcode == 119) buffer.readUnsignedShort() else buffer.readUnsignedByte().toInt()
                soundVolumeSecondary = buffer.readUnsignedByte().toInt()
            }

            20, 120 -> {
                soundRandomRateWide = opcode == 120
                soundRandomRate = mutableListOf(
                    if (opcode == 120) buffer.readUnsignedShort() else buffer.readUnsignedByte().toInt(),
                    buffer.readUnsignedShort(),
                    buffer.readUnsignedShort(),
                )
            }

            22 -> tertiaryVolumeFactor = buffer.readUnsignedByte().toInt()
            23 -> unknown23 = buffer.readUnsignedShort()
            24 -> group = buffer.readUnsignedShort()
            25 -> keyFrameSet = buffer.readUnsignedShort()
            26 -> {
                keyFrameRangeFrom = buffer.readUnsignedShort()
                keyFrameRangeTo = buffer.readUnsignedShort()
            }

            27 -> unknown27 = buffer.readByte().toInt()
            249 -> readParameters(buffer)
        }
    }

    override fun ByteBuf.writePayload(opcode: Int, occurrence: Int, definition: Rs2SequenceType) {
        when (opcode) {
            1 -> {
                val frames = definition.frames!!
                val delays = definition.delays!!
                if (definition.legacyFrames) {
                    val iFrames = definition.legacyIFrames.orEmpty()
                    writeByte(frames.size)
                    for (i in frames.indices) {
                        writeShort(frames[i])
                        writeShort(iFrames.getOrElse(i) { 0 })
                        writeShort(delays[i])
                    }
                } else {
                    writeShort(frames.size)
                    delays.forEach { writeShort(it) }
                    frames.forEach { writeShort(it and 0xFFFF) }
                    frames.forEach { writeShort(it ushr 16) }
                }
            }

            2 -> writeShort(definition.loops!!)
            3 -> {
                val merge = definition.walkMerge!!
                if (wideWalkMerge) writeSmart(merge.size) else writeByte(merge.size)
                merge.forEach { if (wideWalkMerge) writeSmart(it) else writeByte(it) }
            }

            4 -> if (!definition.stretches) writeShort(definition.unknown4!!)
            5 -> writeByte(definition.priority!!)
            6 -> writeShort(definition.replaceHeldLeft!!)
            7 -> writeShort(definition.replaceHeldRight!!)
            8 -> writeByte(definition.maxLoops!!)
            9 -> writeByte(definition.preAnimMove!!)
            10 -> writeByte(definition.postAnimMove!!)
            11 -> writeByte(definition.duplicateBehaviour!!)
            12, 112 -> {
                val iFrames = definition.iFrames!!
                if (opcode == 112) writeShort(iFrames.size) else writeByte(iFrames.size)
                iFrames.forEach { writeShort(it and 0xFFFF) }
                iFrames.forEach { writeShort(it ushr 16) }
            }

            13 -> {
                val sounds = definition.sounds!!
                if (!countedSounds) {
                    writeByte(sounds.size)
                    sounds.forEach { writeMedium(it.packed ?: 0) }
                } else {
                    writeShort(sounds.size)
                    for (sound in sounds) {
                        val packed = sound.packed
                        if (packed == null) {
                            writeByte(0)
                        } else {
                            writeByte(sound.extra.size + 1)
                            writeMedium(packed)
                            sound.extra.forEach { writeShort(it) }
                        }
                    }
                }
            }

            14, 15, 16, 18 -> Unit
            17 -> writeByte(definition.unknown17!!)
            19, 119 -> {
                val volume = definition.soundVolume!!
                if (opcode == 119) writeShort(volume) else writeByte(volume)
                writeByte(definition.soundVolumeSecondary!!)
            }

            20, 120 -> {
                val rate = definition.soundRandomRate!!
                if (opcode == 120) writeShort(rate[0]) else writeByte(rate[0])
                writeShort(rate[1])
                writeShort(rate[2])
            }

            22 -> writeByte(definition.tertiaryVolumeFactor!!)
            23 -> writeShort(definition.unknown23!!)
            24 -> writeShort(definition.group!!)
            25 -> writeShort(definition.keyFrameSet!!)
            26 -> {
                writeShort(definition.keyFrameRangeFrom!!)
                writeShort(definition.keyFrameRangeTo!!)
            }

            27 -> writeByte(definition.unknown27!!)
            249 -> writeParameterBody(this, definition.params)
        }
    }

    override fun createDefinition() = Rs2SequenceType()
}
