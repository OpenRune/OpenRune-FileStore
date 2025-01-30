package dev.openrune.decoder

import dev.openrune.cache.AREA
import dev.openrune.cache.CONFIGS
import dev.openrune.cache.DBROW
import dev.openrune.cache.filestore.buffer.Reader
import dev.openrune.cache.filestore.definition.DefinitionDecoder
import dev.openrune.cache.filestore.definition.data.AreaType
import dev.openrune.cache.filestore.definition.data.DBRowType
import dev.openrune.cache.util.ScriptVarType
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap

class DBRowDecoder : DefinitionDecoder<DBRowType>(CONFIGS) {

    override fun getArchive(id: Int) = DBROW
    override fun createDefinition(): DBRowType = DBRowType()
    override fun getFile(id: Int) = id

    override fun DBRowType.read(opcode: Int, buffer: Reader) {
        when (opcode) {
            3 -> {
                val numColumns = buffer.readUnsignedByte()
                val types = arrayOfNulls<Array<ScriptVarType>?>(numColumns)
                val columnValues = arrayOfNulls<Array<Any?>?>(numColumns)
                var columnId = buffer.readUnsignedByte()
                while (columnId != 255) {
                    val columnTypes = Array(buffer.readUnsignedByte()) {
                        ScriptVarType.forId(buffer.readSmart())!!
                    }
                    types[columnId] = columnTypes
                    columnValues[columnId] = decodeColumnFields(buffer, columnTypes)
                    columnId = buffer.readUnsignedByte()
                }
                columnTypes = types
                this.columnValues = columnValues
            }

            4 -> this.tableId = buffer.readVarInt2()
        }
    }
}

fun Reader.readVarInt2(): Int {
    var value = 0
    var bits = 0
    var read: Int
    do {
        read = readUnsignedByte()
        value = value or (read and 0x7F shl bits)
        bits += 7
    } while (read > 127)
    return value
}