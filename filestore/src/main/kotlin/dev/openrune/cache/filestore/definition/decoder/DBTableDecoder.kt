package dev.openrune.cache.filestore.definition.decoder

import dev.openrune.cache.DBTABLE
import dev.openrune.cache.filestore.definition.DefinitionDecoder
import dev.openrune.cache.filestore.buffer.Reader
import dev.openrune.cache.filestore.definition.data.DBTableType
import dev.openrune.cache.util.ScriptVarType


fun decodeColumnFields(buffer: Reader, types: Array<ScriptVarType>): Array<Any?> {
    val fieldCount = buffer.readSmart()
    val values = arrayOfNulls<Any>(fieldCount * types.size)
    for (fieldIndex in 0 until fieldCount) {
        for (typeIndex in types.indices) {
            val type = types[typeIndex]
            val valuesIndex = fieldIndex * types.size + typeIndex
            values[valuesIndex] = when (type) {
                ScriptVarType.STRING -> buffer.readString()
                else -> buffer.readInt()
            }
        }
    }
    return values
}