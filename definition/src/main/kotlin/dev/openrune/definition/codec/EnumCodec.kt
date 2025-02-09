package dev.openrune.definition.codec

import dev.openrune.buffer.readString
import dev.openrune.definition.DefinitionCodec
import dev.openrune.definition.type.EnumType
import io.netty.buffer.ByteBuf

class EnumCodec : DefinitionCodec<EnumType> {
    override fun EnumType.read(opcode: Int, buffer: ByteBuf) {
        when (opcode) {
            1 -> keyType = buffer.readUnsignedByte().toInt()
            2 -> valueType = buffer.readUnsignedByte().toInt()
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

    override fun ByteBuf.encode(definition: EnumType) {
        TODO("Not yet implemented")
    }

    override fun createDefinition() = EnumType()
}