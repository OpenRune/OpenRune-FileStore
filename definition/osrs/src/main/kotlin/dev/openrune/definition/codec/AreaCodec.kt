package dev.openrune.definition.codec

import dev.openrune.buffer.*
import io.netty.buffer.ByteBuf
import dev.openrune.buffer.Writer
import dev.openrune.buffer.readLargeSmartRD
import dev.openrune.buffer.readStringRD
import dev.openrune.definition.DefinitionCodec
import dev.openrune.definition.type.AreaType

class AreaCodec : DefinitionCodec<AreaType> {

    override fun AreaType.read(opcode: Int, buffer: ByteBuf) {
        when (opcode) {
            1 -> sprite1 = buffer.readLargeSmartRD()
            2 -> sprite2 = buffer.readLargeSmartRD()
            3 -> name = buffer.readStringRD()
            4 -> fontColor = buffer.readMediumRD()
            5 -> buffer.readMediumRD()
            6 -> textSize = buffer.readUnsignedByteRD()
            7 -> {
                val size = buffer.readUnsignedByteRD()
                if ((size and 1) == 0) {
                    renderOnWorldMap = false
                }

                if ((size and 2) == 2) {
                    renderOnMinimap = true
                }
            }
            8 -> buffer.readUnsignedByteRD()
            in 10..14 -> options[opcode - 10] = buffer.readStringRD()
            15 -> {
                val length: Int = buffer.readUnsignedByteRD()
                field1933 = MutableList(length * 2) { 0 }
                (0 until length * 2).forEach {
                    field1933!![it] = buffer.readShortRD()
                }
                buffer.readIntRD()
                val subLength: Int = buffer.readUnsignedByteRD()
                field1930 = MutableList(subLength) { 0 }
                (0 until subLength).forEach {
                    field1930[it] = buffer.readIntRD()
                }
                field1948 = MutableList(length) { 0 }
                (0 until length).forEach {
                    field1948[it] = buffer.readByteRD()
                }
            }
            16 -> buffer.readByteRD()
            17 -> menuTargetName = buffer.readStringRD()
            18 -> buffer.readLargeSmartRD()
            19 -> category = buffer.readUnsignedShortRD()
            21 -> buffer.readIntRD()
            22 -> buffer.readIntRD()
            23 -> buffer.readMediumRD()
            24 -> {
                buffer.readShortRD()
                buffer.readShortRD()
            }
            25 -> buffer.readLargeSmartRD()
            28 -> buffer.readByteRD()
            29 -> horizontalAlignment = buffer.readUnsignedByteRD()
            30 -> verticalAlignment = buffer.readUnsignedByteRD()
        }
    }

    override fun Writer.encode(definition: AreaType) {

        if (definition.sprite1 != -1) {
            writeByte(1)
            writeSmart(definition.sprite1)
        }

        if (definition.sprite2 != -1) {
            writeByte(2)
            writeSmart(definition.sprite2)
        }

        if (definition.name != "null") {
            writeByte(3)
            writeString(definition.name)
        }

        if (definition.fontColor != 0) {
            writeByte(4)
            writeMedium(definition.fontColor)
        }

        if (definition.textSize != 0) {
            writeByte(6)
            writeMedium(definition.textSize)
        }

        if (definition.options.any { it != null }) {
            for (i in definition.options.indices) {
                writeByte(7 + i)
                System.out.println("Op : ${7 + i}")
                if (definition.options[i] == null) {
                    continue
                }
                writeString(definition.options[i]!!)
            }
        }

        if (definition.menuTargetName != "null") {
            writeByte(17)
            writeString(definition.menuTargetName)
        }
        if (definition.category != 0) {
            writeByte(19)
            writeShort(definition.category)
        }

        if (definition.horizontalAlignment != 1) {
            writeByte(29)
            writeByte(definition.horizontalAlignment)
        }

        if (definition.verticalAlignment != 1) {
            writeByte(30)
            writeByte(definition.verticalAlignment)
        }

        writeByte(0)
    }

    override fun createDefinition() = AreaType()
}