package dev.openrune.definition.codec

import dev.openrune.definition.type.HeadbarType
import dev.openrune.definition.util.readNullableLargeSmartCorrect
import dev.openrune.definition.util.writeNullableLargeSmartCorrect
import io.netty.buffer.ByteBuf

class HeadbarCodec : Rs2OrderedCodec<HeadbarType> {

    override fun HeadbarType.read(opcode: Int, buffer: ByteBuf) {
        when (opcode) {
            1 -> unknown1 = buffer.readUnsignedShort()
            2 -> showPriority = buffer.readUnsignedByte().toInt()
            3 -> hidePriority = buffer.readUnsignedByte().toInt()
            4 -> noFadeOut = true
            5 -> stickTime = buffer.readUnsignedShort()
            6 -> unknown6 = buffer.readUnsignedShort()
            7 -> full = buffer.readNullableLargeSmartCorrect() ?: -1
            8 -> empty = buffer.readNullableLargeSmartCorrect() ?: -1
            9 -> fullPlayerGroup = buffer.readNullableLargeSmartCorrect() ?: -1
            10 -> emptyPlayerGroup = buffer.readNullableLargeSmartCorrect() ?: -1
            11 -> fadeOut = buffer.readUnsignedShort()
            12 -> fullPlayerGroupTeam = buffer.readNullableLargeSmartCorrect() ?: -1
            13 -> emptyPlayerGroupTeam = buffer.readNullableLargeSmartCorrect() ?: -1
            14 -> leftCapSprite = buffer.readNullableLargeSmartCorrect() ?: -1
            15 -> rightCapSprite = buffer.readNullableLargeSmartCorrect() ?: -1
            16 -> tileFill = true
            17 -> stackOffset = buffer.readUnsignedByte().toInt()
        }
    }

    override fun ByteBuf.writePayload(opcode: Int, occurrence: Int, definition: HeadbarType) {
        when (opcode) {
            1 -> writeShort(definition.unknown1!!)
            2 -> writeByte(definition.showPriority!!)
            3 -> writeByte(definition.hidePriority!!)
            4 -> Unit
            5 -> writeShort(definition.stickTime!!)
            6 -> writeShort(definition.unknown6!!)
            7 -> writeNullableLargeSmartCorrect(definition.full)
            8 -> writeNullableLargeSmartCorrect(definition.empty)
            9 -> writeNullableLargeSmartCorrect(definition.fullPlayerGroup)
            10 -> writeNullableLargeSmartCorrect(definition.emptyPlayerGroup)
            11 -> writeShort(definition.fadeOut!!)
            12 -> writeNullableLargeSmartCorrect(definition.fullPlayerGroupTeam)
            13 -> writeNullableLargeSmartCorrect(definition.emptyPlayerGroupTeam)
            14 -> writeNullableLargeSmartCorrect(definition.leftCapSprite)
            15 -> writeNullableLargeSmartCorrect(definition.rightCapSprite)
            16 -> Unit
            17 -> writeByte(definition.stackOffset!!)
        }
    }

    override fun createDefinition() = HeadbarType()
}
