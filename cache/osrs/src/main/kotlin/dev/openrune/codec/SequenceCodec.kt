package dev.openrune.codec

import dev.openrune.OsrsCacheProvider.Companion.CACHE_REVISION
import dev.openrune.cache.CacheManager
import dev.openrune.cache.filestore.buffer.Reader
import dev.openrune.cache.filestore.buffer.Writer
import dev.openrune.cache.filestore.definition.DefinitionCodec
import dev.openrune.cache.filestore.definition.data.SequenceType
import kotlin.math.ceil

class SequenceCodec : DefinitionCodec<SequenceType> {
    override fun SequenceType.read(opcode: Int, buffer: Reader) {
        when (opcode) {
            1 -> {
                val frameCount = buffer.readUnsignedShort()
                var totalFrameLength = 0
                frameIDs = MutableList(frameCount) { 0 }
                frameDelays = MutableList(frameCount) { 0 }

                for (i in 0 until frameCount) {
                    frameDelays!![i] = buffer.readUnsignedShort()
                    if (i < frameCount - 1 || frameDelays!![i] < 200) {
                        totalFrameLength += frameDelays!![i]
                    }
                }

                for (i in 0 until frameCount) {
                    frameIDs!![i] = buffer.readUnsignedShort()
                }

                for (i in 0 until frameCount) {
                    frameIDs!![i] += buffer.readUnsignedShort() shl 16
                }

                lengthInCycles = ceil((totalFrameLength * 20.0) / 600.0).toInt()
            }

            2 -> frameStep = buffer.readUnsignedShort()
            3 -> {
                val count = buffer.readUnsignedByte()
                interleaveLeave = MutableList(count + 1) { 0 }
                for (i in 0 until count) {
                    interleaveLeave!![i] = buffer.readUnsignedByte()
                }
                interleaveLeave!![count] = 0x98967f
            }

            4 -> stretches = true
            5 -> forcedPriority = buffer.readUnsignedByte()
            6 -> leftHandItem = buffer.readUnsignedShort()
            7 -> rightHandItem = buffer.readUnsignedShort()
            8 -> maxLoops = buffer.readUnsignedByte()
            9 -> precedenceAnimating = buffer.readUnsignedByte()
            10 -> priority = buffer.readUnsignedByte()
            11 -> replyMode = buffer.readUnsignedByte()
            12 -> {
                val count = buffer.readUnsignedByte()
                chatFrameIds = MutableList(count) { 0 }
                for (i in 0 until count) {
                    chatFrameIds!![i] = buffer.readUnsignedShort()
                }

                for (i in 0 until count) {
                    chatFrameIds!![i] += buffer.readUnsignedShort() shl 16
                }
            }

            13 -> {
                val count = buffer.readUnsignedByte()
                soundEffects = MutableList(count) { null }
                for (i in 0 until count) {
                    soundEffects[i] = readSounds(buffer, CacheManager.revisionIsOrAfter(CACHE_REVISION,220))
                }
            }

            14 -> skeletalId = buffer.readInt()
            15 -> {
                val count = buffer.readUnsignedShort()
                for (i in 0 until count) {
                    val index = buffer.readUnsignedShort()
                    val sound = readSounds(buffer, CacheManager.revisionIsOrAfter(CACHE_REVISION,220))
                    skeletalSounds[index] = sound!!
                }
            }

            16 -> {
                rangeBegin = buffer.readUnsignedShort()
                rangeEnd = buffer.readUnsignedShort()
            }

            17 -> {
                mask = MutableList(256) { false }
                val count = buffer.readUnsignedByte()
                for (i in 0 until count) {
                    mask!![buffer.readUnsignedByte()] = true
                }
            }
        }
    }

    override fun Writer.encode(definition: SequenceType) {

        if (definition.frameIDs != null) {
            writeByte(1)
            writeShort(definition.frameIDs!!.size)
            for (i in 0 until definition.frameDelays!!.size) {
                writeShort(definition.frameDelays!![i])
            }
            for (i in 0 until definition.frameIDs!!.size) {
                writeShort(definition.frameIDs!![i])
            }
            for (i in 0 until definition.frameIDs!!.size) {
                writeShort(definition.frameIDs!![i] shr 16)
            }
        }

        if (definition.frameStep != -1) {
            writeByte(2)
            writeShort(definition.frameStep)
        }

        if (definition.interleaveLeave != null) {
            writeByte(3)
            writeByte(definition.interleaveLeave!!.size)
            for (i in 0 until definition.interleaveLeave!!.size) {
                writeByte(definition.interleaveLeave!![i])
            }
        }

        if (definition.stretches) {
            writeByte(4)
        }

        if (definition.forcedPriority != 5) {
            writeByte(5)
            writeByte(definition.forcedPriority)
        }

        if (definition.leftHandItem != -1) {
            writeByte(6)
            writeShort(definition.leftHandItem)
        }

        if (definition.rightHandItem != -1) {
            writeByte(7)
            writeShort(definition.rightHandItem)
        }

        if (definition.maxLoops != 99) {
            writeByte(8)
            writeByte(definition.maxLoops)
        }

        if (definition.precedenceAnimating != -1) {
            writeByte(9)
            writeByte(definition.precedenceAnimating)
        }

        if (definition.priority != -1) {
            writeByte(10)
            writeByte(definition.priority)
        }

        if (definition.replyMode != 2) {
            writeByte(11)
            writeByte(definition.replyMode)
        }

        if (definition.chatFrameIds != null) {
            writeByte(12)
            writeByte(definition.chatFrameIds!!.size)

            for (i in 0 until definition.chatFrameIds!!.size) {
                writeShort(definition.chatFrameIds!![i])
            }
            for (i in 0 until definition.chatFrameIds!!.size) {
                writeShort(definition.chatFrameIds!![i] shr 16)
            }

        }

        if (definition.soundEffects.isNotEmpty()) {
            writeByte(13)
            writeByte(definition.soundEffects.size)
            definition.soundEffects.forEach {
                it!!.writeSound(this, CacheManager.revisionIsOrAfter(CACHE_REVISION,220))
            }
        }

        if (definition.skeletalId != -1) {
            writeByte(14)
            writeInt(definition.skeletalId)
        }

        if (definition.skeletalSounds.isNotEmpty()) {
            writeByte(15)
            writeShort(definition.skeletalSounds.size)
            definition.skeletalSounds.forEach { (index, sound) ->
                writeShort(index)
                sound.writeSound(this, CacheManager.revisionIsOrAfter(CACHE_REVISION,220))
            }
        }

        if (definition.rangeBegin != 0 || definition.rangeEnd != 0) {
            writeByte(16)
            writeShort(definition.rangeBegin)
            writeShort(definition.rangeEnd)
        }

        if (definition.mask != null) {
            writeByte(17)

            writeByte(definition.mask!!.filter { it }.size)
            definition.mask!!.forEachIndexed { index, state ->
                if (state) {
                    writeByte(index)
                }
            }

        }

        writeByte(0)
    }
}