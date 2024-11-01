package dev.openrune.cache.filestore.definition.decoder

import dev.openrune.cache.*
import dev.openrune.cache.filestore.definition.DefinitionDecoder
import dev.openrune.cache.filestore.buffer.Reader
import dev.openrune.cache.filestore.definition.data.OverlayType
import dev.openrune.cache.filestore.definition.data.ParamType
import dev.openrune.cache.filestore.definition.data.StructType
import dev.openrune.cache.util.ScriptVarType
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap

class ParamDecoder : DefinitionDecoder<ParamType>(CONFIGS) {

    override fun getArchive(id: Int) = PARAMS

    override fun create(): Int2ObjectOpenHashMap<ParamType> = createMap { ParamType(it) }

    override fun getFile(id: Int) = id

    override fun ParamType.read(opcode: Int, buffer: Reader) {
        when (opcode) {
            1 -> {
                val idx = buffer.readUnsignedByte()
                ScriptVarType.forCharKey(idx.toChar())?.let { values.put("type", it) }
            }

            2 -> values["defaultInt"] = buffer.readInt()
            4 -> values["isMembers"] = false
            5 -> values["defaultString"] = buffer.readString()
        }
    }
}