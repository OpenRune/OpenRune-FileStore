package dev.openrune.codec

import dev.openrune.cache.filestore.buffer.Reader
import dev.openrune.cache.filestore.buffer.Writer
import dev.openrune.cache.filestore.definition.DefinitionCodec
import dev.openrune.cache.filestore.definition.data.ParamType
import dev.openrune.cache.util.ScriptVarType

class ParamCodec : DefinitionCodec<ParamType> {
    override fun ParamType.read(opcode: Int, buffer: Reader) {
        when (opcode) {
            1 -> {
                val idx = buffer.readUnsignedByte()
                type = ScriptVarType.forCharKey(idx.toChar())
            }

            2 -> defaultInt = buffer.readInt()
            4 -> isMembers = false
            5 -> defaultString = buffer.readString()
        }
    }

    override fun Writer.encode(definition: ParamType) {
        if(definition.type != null) {
            writeByte(1)
            writeByte(definition.type!!.keyChar.code)
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
}