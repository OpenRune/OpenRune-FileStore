package dev.openrune.definition.codec.new

import dev.openrune.definition.DefinitionCodec
import dev.openrune.definition.revisionIsOrAfter
import dev.openrune.definition.type.SequenceType
import dev.openrune.definition.opcode.DefinitionOpcode
import dev.openrune.definition.opcode.OpcodeDefinitionCodec
import dev.openrune.definition.opcode.OpcodeList
import dev.openrune.definition.opcode.OpcodeType
import dev.openrune.definition.opcode.impl.DefinitionOpcodeList
import io.netty.buffer.ByteBuf

class SequenceCodecNew(private val revision: Int) : OpcodeDefinitionCodec<SequenceType>() {

    private val skeletalIdOpcode: Int
    private val skeletalSoundOpcode: Int
    private val skeletalRangeOpcode: Int

    init {
        if (revision < 226) {
            skeletalIdOpcode = 14
            skeletalSoundOpcode = 15
            skeletalRangeOpcode = 16
        } else {
            skeletalIdOpcode = 13
            skeletalSoundOpcode = 14
            skeletalRangeOpcode = 15
        }
    }

    override val definitionCodec = OpcodeList<SequenceType>().apply {
        add(DefinitionOpcode(1,
            decode = { buf, def, _ ->
                val frameCount = buf.readUnsignedShort()
                def.frameIDs = MutableList(frameCount) { 0 }
                def.frameDelays = MutableList(frameCount) { 0 }

                for (i in 0 until frameCount) {
                    def.frameDelays!![i] = buf.readUnsignedShort()
                }

                for (i in 0 until frameCount) {
                    def.frameIDs!![i] = buf.readUnsignedShort()
                }

                for (i in 0 until frameCount) {
                    def.frameIDs!![i] += buf.readUnsignedShort() shl 16
                }
            },
            encode = { buf, def ->
                val frameIDs = def.frameIDs
                val frameDelays = def.frameDelays
                if (frameIDs != null && frameDelays != null) {
                    buf.writeShort(frameIDs.size)
                    for (i in 0 until frameDelays.size) {
                        buf.writeShort(frameDelays[i])
                    }
                    for (i in 0 until frameIDs.size) {
                        buf.writeShort(frameIDs[i])
                    }
                    for (i in 0 until frameIDs.size) {
                        buf.writeShort(frameIDs[i] shr 16)
                    }
                }
            },
            shouldEncode = { it.frameIDs != null }
        ))

        add(DefinitionOpcode(2, OpcodeType.USHORT, SequenceType::frameStep))

        add(DefinitionOpcode(3,
            decode = { buf, def, _ ->
                val count = buf.readUnsignedByte().toInt()
                def.interleaveLeave = MutableList(count + 1) { 0 }
                for (i in 0 until count) {
                    def.interleaveLeave!![i] = buf.readUnsignedByte().toInt()
                }
                def.interleaveLeave!![count] = 0x98967f
            },
            encode = { buf, def ->
                val interleave = def.interleaveLeave
                if (interleave != null) {
                    buf.writeByte(interleave.size - 1)
                    for (i in 0 until interleave.size - 1) {
                        buf.writeByte(interleave[i])
                    }
                }
            },
            shouldEncode = { it.interleaveLeave != null }
        ))

        add(DefinitionOpcode(4, SequenceType::stretches))

        add(DefinitionOpcode(5, OpcodeType.UBYTE, SequenceType::forcedPriority))

        add(DefinitionOpcode(6, OpcodeType.USHORT, SequenceType::leftHandItem))

        add(DefinitionOpcode(7, OpcodeType.USHORT, SequenceType::rightHandItem))

        add(DefinitionOpcode(8, OpcodeType.UBYTE, SequenceType::maxLoops))

        add(DefinitionOpcode(9, OpcodeType.UBYTE, SequenceType::precedenceAnimating))

        add(DefinitionOpcode(10, OpcodeType.UBYTE, SequenceType::priority))

        add(DefinitionOpcode(11, OpcodeType.UBYTE, SequenceType::replyMode))

        add(DefinitionOpcode(12,
            decode = { buf, def, _ ->
                val count = buf.readUnsignedByte().toInt()
                def.chatFrameIds = MutableList(count) { 0 }
                for (i in 0 until count) {
                    def.chatFrameIds!![i] = buf.readUnsignedShort()
                }

                for (i in 0 until count) {
                    def.chatFrameIds!![i] += buf.readUnsignedShort() shl 16
                }
            },
            encode = { buf, def ->
                val chatFrameIds = def.chatFrameIds
                if (chatFrameIds != null) {
                    buf.writeByte(chatFrameIds.size)
                    for (i in 0 until chatFrameIds.size) {
                        buf.writeShort(chatFrameIds[i])
                    }
                    for (i in 0 until chatFrameIds.size) {
                        buf.writeShort(chatFrameIds[i] shr 16)
                    }
                }
            },
            shouldEncode = { it.chatFrameIds != null }
        ))

        addIfRevisionBefore(revision, 226, DefinitionOpcode(13,
            decode = { buf, def, _ ->
                val count = buf.readUnsignedByte().toInt()
                def.soundEffects = MutableList(count) { null }
                for (i in 0 until count) {
                    def.soundEffects[i] = def.readSounds(buf, revision)
                }
            },
            encode = { buf, def ->
                if (def.soundEffects.isNotEmpty()) {
                    buf.writeByte(def.soundEffects.size)
                    def.soundEffects.forEach {
                        it!!.writeSound(buf, revision)
                    }
                }
            },
            shouldEncode = { it.soundEffects.isNotEmpty() }
        ))

        add(DefinitionOpcode(skeletalIdOpcode, OpcodeType.INT, SequenceType::skeletalId))

        add(DefinitionOpcode(skeletalSoundOpcode,
            decode = { buf, def, _ ->
                val count = buf.readUnsignedShort()
                for (i in 0 until count) {
                    val index = buf.readUnsignedShort()
                    val sound = def.readSounds(buf, revision)
                    if (sound != null) {
                        def.skeletalSounds[index] = sound
                    }
                }
            },
            encode = { buf, def ->
                if (def.skeletalSounds.isNotEmpty()) {
                    buf.writeShort(def.skeletalSounds.size)
                    def.skeletalSounds.forEach { (index, sound) ->
                        buf.writeShort(index)
                        sound.writeSound(buf, revision)
                    }
                }
            },
            shouldEncode = { it.skeletalSounds.isNotEmpty() }
        ))

        add(DefinitionOpcode(skeletalRangeOpcode,
            decode = { buf, def, _ ->
                def.rangeBegin = buf.readUnsignedShort()
                def.rangeEnd = buf.readUnsignedShort()
            },
            encode = { buf, def ->
                buf.writeShort(def.rangeBegin)
                buf.writeShort(def.rangeEnd)
            },
            shouldEncode = { it.rangeBegin != 0 || it.rangeEnd != 0 }
        ))

        addIfRevisionAfter(revision, 226, DefinitionOpcode(16, OpcodeType.BYTE, SequenceType::verticalOffset))

        add(DefinitionOpcode(17,
            decode = { buf, def, _ ->
                def.mask = MutableList(256) { false }
                val count = buf.readUnsignedByte().toInt()
                for (i in 0 until count) {
                    def.mask!![buf.readUnsignedByte().toInt()] = true
                }
            },
            encode = { buf, def ->
                val mask = def.mask
                if (mask != null) {
                    val trueIndices = mask.withIndex().filter { it.value }.map { it.index }
                    buf.writeByte(trueIndices.size)
                    trueIndices.forEach { buf.writeByte(it) }
                }
            },
            shouldEncode = { it.mask != null }
        ))

        add(DefinitionOpcode(18, OpcodeType.STRING, SequenceType::debugName))
    }

    override fun createDefinition() = SequenceType()
}

