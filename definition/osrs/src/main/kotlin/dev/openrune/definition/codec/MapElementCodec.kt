package dev.openrune.definition.codec

import dev.openrune.definition.DefinitionCodec
import dev.openrune.definition.type.MapElementType
import dev.openrune.definition.util.*
import io.netty.buffer.ByteBuf

class MapElementCodec : DefinitionCodec<MapElementType> {

    override fun MapElementType.read(opcode: Int, buffer: ByteBuf) {
        when (opcode) {
            1 -> sprite1 = buffer.readNullableLargeSmart()
            2 -> sprite2 = buffer.readNullableLargeSmart()
            3 -> name = buffer.readString()
            4 -> fontColor = buffer.readUnsignedMedium()
            5 -> buffer.readUnsignedMedium()
            6 -> textSize = buffer.readUnsignedByte().toInt()
            7 -> {
                val size = buffer.readUnsignedByte().toInt()
                if ((size and 1) == 0) {
                    renderOnWorldMap = false
                }

                if ((size and 2) == 2) {
                    renderOnMinimap = true
                }
            }

            8 -> buffer.readUnsignedByte().toInt()
            in 10..14 -> options[opcode - 10] = buffer.readString()
            15 -> {
                val length: Int = buffer.readUnsignedByte().toInt()
                field1933 = MutableList(length * 2) { 0 }
                (0 until length * 2).forEach {
                    field1933!![it] = buffer.readShort().toInt()
                }
                buffer.readInt()
                val subLength: Int = buffer.readUnsignedByte().toInt()
                field1930 = MutableList(subLength) { 0 }
                (0 until subLength).forEach {
                    field1930[it] = buffer.readInt()
                }
                field1948 = MutableList(length) { 0 }
                (0 until length).forEach {
                    field1948[it] = buffer.readByte().toInt()
                }
            }

            16 -> buffer.readByte().toInt()
            17 -> menuTargetName = buffer.readString()
            18 -> buffer.readNullableLargeSmart()
            19 -> category = buffer.readUnsignedShort()
            21 -> buffer.readInt()
            22 -> buffer.readInt()
            23 -> buffer.readUnsignedMedium()
            24 -> {
                buffer.readShort().toInt()
                buffer.readShort().toInt()
            }

            25 -> buffer.readNullableLargeSmart()
            28 -> buffer.readByte().toInt()
            29 -> horizontalAlignment = buffer.readUnsignedByte().toInt()
            30 -> verticalAlignment = buffer.readUnsignedByte().toInt()
        }
    }

    override fun ByteBuf.encode(definition: MapElementType) {

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

    override fun createDefinition() = MapElementType()
}