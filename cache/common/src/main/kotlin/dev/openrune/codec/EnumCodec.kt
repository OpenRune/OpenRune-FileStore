package dev.openrune.codec

import dev.openrune.cache.filestore.buffer.Reader
import dev.openrune.cache.filestore.buffer.Writer
import dev.openrune.cache.filestore.definition.DefinitionCodec
import dev.openrune.cache.filestore.definition.data.EnumType

class EnumCodec : DefinitionCodec<EnumType> {
    override fun EnumType.read(opcode: Int, buffer: Reader) {
        when (opcode) {
            1 -> keyType = buffer.readUnsignedByte()
            2 -> valueType = buffer.readUnsignedByte()
            3 -> defaultString = buffer.readString()
            4 -> defaultInt = buffer.readInt()
            5, 6 -> {
                val count = buffer.readUnsignedShort()
                for (i in 0 until count) {
                    val key = buffer.readInt()
                    if (opcode == 5) {
                        values[key] = buffer.readString()
                    } else {
                        values[key] = buffer.readInt()
                    }
                }
            }
        }
    }

    override fun Writer.encode(definition: EnumType) {
        TODO("Not yet implemented")
    }

    override fun createDefinition() = EnumType()
}