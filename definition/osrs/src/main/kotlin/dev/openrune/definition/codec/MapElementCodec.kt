package dev.openrune.definition.codec

import dev.openrune.definition.BuilderDefinitionCodec
import dev.openrune.definition.type.MapElementType
import dev.openrune.definition.type.builders.MapElementTypeBuilder
import dev.openrune.definition.util.*
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled

class MapElementCodec : BuilderDefinitionCodec<MapElementType, MapElementTypeBuilder> {

    override fun builder(id: Int) = MapElementTypeBuilder(id)

    override fun build(builder: MapElementTypeBuilder) = builder.build()

    override fun MapElementTypeBuilder.read(opcode: Int, buffer: ByteBuf) {
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
                field1933 = readIntList(length * 2) { buffer.readShort().toInt() }
                buffer.readInt()
                val subLength: Int = buffer.readUnsignedByte().toInt()
                field1930 = readIntList(subLength) { buffer.readInt() }
                field1948 = readIntList(length) { buffer.readByte().toInt() }
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
            writeByte(definition.textSize)
        }

        if (!definition.renderOnWorldMap || definition.renderOnMinimap) {
            writeByte(7)
            var flags = 0
            if (definition.renderOnWorldMap) {
                flags = flags or 1
            }
            if (definition.renderOnMinimap) {
                flags = flags or 2
            }
            writeByte(flags)
        }

        for (i in definition.options.indices) {
            val option = definition.options[i] ?: continue
            writeByte(10 + i)
            writeString(option)
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
