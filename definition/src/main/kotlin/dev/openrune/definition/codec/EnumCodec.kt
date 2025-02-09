package dev.openrune.definition.codec

import dev.openrune.buffer.*
import dev.openrune.definition.DefinitionCodec
import dev.openrune.definition.type.EnumType
import io.netty.buffer.ByteBuf

class EnumCodec : DefinitionCodec<EnumType> {
    override fun EnumType.read(opcode: Int, buffer: ByteBuf) {
        when (opcode) {
            1 -> keyType = buffer.readUnsignedByteRD()
            2 -> valueType = buffer.readUnsignedByteRD()
            3 -> defaultString = buffer.readStringRD()
            4 -> defaultInt = buffer.readIntRD()
            5, 6 -> {
                val count = buffer.readUnsignedShortRD()
                for (i in 0 until count) {
                    val key = buffer.readIntRD()
                    if (opcode == 5) {
                        values[key] = buffer.readStringRD()
                    } else {
                        values[key] = buffer.readIntRD()
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