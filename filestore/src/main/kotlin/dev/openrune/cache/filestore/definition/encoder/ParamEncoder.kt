package dev.openrune.cache.filestore.definition.encoder

import dev.openrune.cache.filestore.buffer.Writer
import dev.openrune.cache.filestore.definition.ConfigEncoder
import dev.openrune.cache.filestore.definition.data.ParamType
import dev.openrune.cache.filestore.definition.data.StructType

class ParamEncoder: ConfigEncoder<ParamType>() {
    override fun Writer.encode(definition: ParamType) {
        val type = definition.getType()
        if(type != null) {
            writeByte(1)
            writeByte(type.keyChar.code)
        }
        val defaultInt = definition.getDefaultInt()
        if(defaultInt != 0) {
            writeByte(3)
            writeInt(defaultInt)
        }
        val isMembers = definition.isMembers()
        if(isMembers) {
            writeByte(4)
        }
        val defaultString = definition.getDefaultString()
        if(defaultString != null) {
            writeByte(5)
            writeString(defaultString)
        }
        writeByte(0)
    }
}