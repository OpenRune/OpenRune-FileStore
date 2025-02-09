package dev.openrune.definition.codec

import dev.openrune.buffer.*
import dev.openrune.definition.DefinitionCodec
import dev.openrune.definition.type.ParamType
import dev.openrune.definition.util.Type
import io.netty.buffer.ByteBuf

class ParamCodec : DefinitionCodec<ParamType> {
    override fun ParamType.read(opcode: Int, buffer: ByteBuf) {
        when (opcode) {
            1 -> {
                val idx = buffer.readUnsignedByteRD()
                type = Type.byChar(idx.toChar())
            }

            2 -> defaultInt = buffer.readIntRD()
            4 -> isMembers = false
            5 -> defaultString = buffer.readStringRD()
        }
    }

    override fun Writer.encode(definition: ParamType) {
        if(definition.type != null) {
            writeByte(1)
            writeByte(definition.type!!.ch.code)
        }
        if(definition.defaultInt != 0) {
            writeByte(3)
            writeInt(definition.defaultInt)
        }
        if(definition.isMembers) {
            writeByte(4)
        }
        if(definition.defaultString != null) {
            writeByte(5)
            writeString(definition.defaultString)
        }
        writeByte(0)
    }

    override fun createDefinition() = ParamType()
}