package dev.openrune.definition.codec

import dev.openrune.definition.util.readShortSmart
import dev.openrune.definition.util.readString
import dev.openrune.definition.util.writePrefixedString
import dev.openrune.definition.DefinitionCodec
import dev.openrune.definition.type.HitSplatType
import io.netty.buffer.ByteBuf

class HitSplatCodec : DefinitionCodec<HitSplatType> {
    override fun HitSplatType.read(opcode: Int, buffer: ByteBuf) {
        when (opcode) {
            1 -> font = buffer.readShortSmart()
            2 -> textColour = buffer.readUnsignedMedium()
            3 -> icon = buffer.readShortSmart()
            4 -> left = buffer.readShortSmart()
            5 -> middle = buffer.readShortSmart()
            6 -> right = buffer.readShortSmart()
            7 -> offsetX = buffer.readUnsignedShort()
            8 -> amount = buffer.readString()
            9 -> duration = buffer.readUnsignedShort()
            10 -> offsetY = buffer.readShort().toInt()
            11 -> fade = 0
            12 -> comparisonType = buffer.readUnsignedByte().toInt()
            13 -> damageYOfset = buffer.readShort().toInt()
            14 -> fade = buffer.readShort().toInt()
            17, 18 -> readTransforms(buffer, opcode == 18)
        }
    }

    override fun ByteBuf.encode(definition: HitSplatType) {
        if (definition.font != -1) {
            writeByte(1)
            writeShort(definition.font)
        }
        if (definition.textColour != 16777215) {
            writeByte(2)
            writeMedium(definition.textColour)
        }
        if (definition.icon != -1) {
            writeByte(3)
            writeShort(definition.icon)
        }
        if (definition.left != -1) {
            writeByte(4)
            writeShort(definition.left)
        }
        if (definition.middle != -1) {
            writeByte(5)
            writeShort(definition.middle)
        }
        if (definition.right != -1) {
            writeByte(6)
            writeShort(definition.right)
        }
        if (definition.offsetX != 0) {
            writeByte(7)
            writeShort(definition.offsetX)
        }
        if (definition.amount != "") {
            writeByte(8)
            writePrefixedString(definition.amount)
        }
        if (definition.duration != 70) {
            writeByte(9)
            writeShort(definition.duration)
        }
        if (definition.offsetY != 0) {
            writeByte(10)
            writeShort(definition.offsetY)
        }

        if (definition.fade != -1) {
            writeByte(11)
        }

        if (definition.comparisonType != -1) {
            writeByte(12)
            writeByte(definition.comparisonType)
        }

        if (definition.damageYOfset != 0) {
            writeByte(13)
            writeShort(definition.damageYOfset)
        }

        if (definition.fade != 0) {
            writeByte(14)
            writeShort(definition.fade)
        }

        definition.writeTransforms(this, 17, 18)

        writeByte(0)
    }

    override fun createDefinition() = HitSplatType()
}