package dev.openrune.definition.codec

import dev.openrune.definition.DefinitionCodec
import dev.openrune.definition.type.SequenceType
import dev.openrune.definition.util.readString
import dev.openrune.definition.util.writeString
import io.netty.buffer.ByteBuf

class SequenceCodec(private val revision: Int) : DefinitionCodec<SequenceType> {

    private val frameSoundOpcode: Int
    private val skeletalIdOpcode: Int
    private val skeletalSoundOpcode: Int
    private val skeletalRangeOpcode: Int

    init {
        if(revision < 226) {
            frameSoundOpcode = 13
            skeletalIdOpcode = 14
            skeletalSoundOpcode = 15
            skeletalRangeOpcode = 16
        } else {
            frameSoundOpcode = -1// removed in 226
            skeletalIdOpcode = 13
            skeletalSoundOpcode = 14
            skeletalRangeOpcode = 15
        }
    }

    override fun SequenceType.read(opcode: Int, buffer: ByteBuf) {
        when (opcode) {
            1 -> {
                val frameCount = buffer.readUnsignedShort()
                frameIDs = MutableList(frameCount) { 0 }
                frameDelays = MutableList(frameCount) { 0 }

                for (i in 0 until frameCount) {
                    frameDelays!![i] = buffer.readUnsignedShort()
                }

                for (i in 0 until frameCount) {
                    frameIDs!![i] = buffer.readUnsignedShort()
                }

                for (i in 0 until frameCount) {
                    frameIDs!![i] += buffer.readUnsignedShort() shl 16
                }

            }

            2 -> frameStep = buffer.readUnsignedShort()
            3 -> {
                val count = buffer.readUnsignedByte().toInt()
                interleaveLeave = MutableList(count + 1) { 0 }
                for (i in 0 until count) {
                    interleaveLeave!![i] = buffer.readUnsignedByte().toInt()
                }
                interleaveLeave!![count] = 0x98967f
            }

            4 -> stretches = true
            5 -> forcedPriority = buffer.readUnsignedByte().toInt()
            6 -> leftHandItem = buffer.readUnsignedShort()
            7 -> rightHandItem = buffer.readUnsignedShort()
            8 -> maxLoops = buffer.readUnsignedByte().toInt()
            9 -> precedenceAnimating = buffer.readUnsignedByte().toInt()
            10 -> priority = buffer.readUnsignedByte().toInt()
            11 -> replyMode = buffer.readUnsignedByte().toInt()
            12 -> {
                val count = buffer.readUnsignedByte().toInt()
                chatFrameIds = MutableList(count) { 0 }
                for (i in 0 until count) {
                    chatFrameIds!![i] = buffer.readUnsignedShort()
                }

                for (i in 0 until count) {
                    chatFrameIds!![i] += buffer.readUnsignedShort() shl 16
                }
            }

            frameSoundOpcode -> {
                val count = buffer.readUnsignedByte().toInt()
                soundEffects = MutableList(count) { null }
                for (i in 0 until count) {
                    soundEffects[i] = readSounds(buffer, revision)
                }
            }

            skeletalIdOpcode -> skeletalId = buffer.readInt()
            skeletalSoundOpcode -> {
                val count = buffer.readUnsignedShort()
                for (i in 0 until count) {
                    val index = buffer.readUnsignedShort()
                    val sound = readSounds(buffer, revision)
                    skeletalSounds[index] = sound!!
                }
            }

            skeletalRangeOpcode -> {
                rangeBegin = buffer.readUnsignedShort()
                rangeEnd = buffer.readUnsignedShort()
            }

            16 -> verticalOffset = buffer.readByte().toInt()
            17 -> {
                mask = MutableList(256) { false }
                val count = buffer.readUnsignedByte().toInt()
                for (i in 0 until count) {
                    mask!![buffer.readUnsignedByte().toInt()] = true
                }
            }
            18 -> debugName = buffer.readString()
        }
    }

    override fun ByteBuf.encode(definition: SequenceType) {

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

        if (definition.soundEffects.isNotEmpty() && revision < 226) {
            writeByte(frameSoundOpcode)
            writeByte(definition.soundEffects.size)
            definition.soundEffects.forEach {
                it!!.writeSound(this, revision)
            }
        }

        if (definition.skeletalId != -1) {
            writeByte(skeletalIdOpcode)
            writeInt(definition.skeletalId)
        }

        if (definition.skeletalSounds.isNotEmpty()) {
            writeByte(skeletalSoundOpcode)
            writeShort(definition.skeletalSounds.size)
            definition.skeletalSounds.forEach { (index, sound) ->
                writeShort(index)
                sound.writeSound(this, revision)
            }
        }

        if (definition.rangeBegin != 0 || definition.rangeEnd != 0) {
            writeByte(skeletalRangeOpcode)
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

        if (definition.debugName.isNotEmpty()) {
            writeByte(18)
            writeString(definition.debugName)
        }

        writeByte(0)
    }

    override fun createDefinition() = SequenceType()
}