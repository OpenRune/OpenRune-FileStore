package dev.openrune.codec

import dev.openrune.cache.filestore.buffer.Reader
import dev.openrune.cache.filestore.buffer.Writer
import dev.openrune.cache.filestore.definition.DefinitionCodec
import dev.openrune.cache.filestore.definition.data.HealthBarType

class HealthBarCodec : DefinitionCodec<HealthBarType> {
    override fun HealthBarType.read(opcode: Int, buffer: Reader) {
        when (opcode) {
            1 -> buffer.readUnsignedShort()
            2 -> int1 = buffer.readUnsignedByte()
            3 -> int2 = buffer.readUnsignedByte()
            4 -> int3 = 0
            5 -> int4 = buffer.readUnsignedShort()
            6 -> buffer.readUnsignedByte()
            7 -> frontSpriteId = buffer.readUnsignedShort()
            8 -> backSpriteId = buffer.readUnsignedShort()
            11 -> int3 = buffer.readUnsignedShort()
            14 -> width = buffer.readUnsignedByte()
            15 -> widthPadding = buffer.readUnsignedByte()
        }
    }

    override fun Writer.encode(definition: HealthBarType) {
        if (definition.int1 != 255) {
            writeByte(2)
            writeByte(definition.int1)
        }
        if (definition.int2 != 255) {
            writeByte(3)
            writeByte(definition.int2)
        }
        if (definition.int3 != -1) {
            writeByte(4)
        }
        if (definition.int4 != 70) {
            writeByte(5)
            writeShort(definition.int4)
        }

        if (definition.frontSpriteId != -1) {
            writeByte(7)
            writeShort(definition.frontSpriteId)
        }
        if (definition.backSpriteId != -1) {
            writeByte(8)
            writeShort(definition.backSpriteId)
        }
        if (definition.int3 != -1) {
            writeByte(11)
            writeShort(definition.int3)
        }
        if (definition.width != 30) {
            writeByte(14)
            writeByte(definition.width)
        }

        if (definition.widthPadding != 0) {
            writeByte(15)
            writeByte(definition.widthPadding)
        }

        writeByte(0)
    }

    override fun createDefinition() = HealthBarType()
}