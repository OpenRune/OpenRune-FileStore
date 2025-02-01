package dev.openrune.definition.codec

import dev.openrune.buffer.Reader
import dev.openrune.buffer.Writer
import dev.openrune.definition.DefinitionCodec
import dev.openrune.definition.type.DBRowType
import dev.openrune.definition.util.ScriptVarType

class DBRowCodec : DefinitionCodec<DBRowType> {
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

    override fun Writer.encode(definition: DBRowType) {
        when {
            definition.columnTypes != null -> {
                writeByte(3)
                val types = definition.columnTypes
                val columnValues = definition.columnValues
                writeByte(types!!.size)
                types.indices.forEach { columnId ->
                    val columnTypes = types[columnId] ?: return@forEach
                    writeByte(columnId)
                    writeByte(columnTypes.size)
                    for (type in columnTypes) {
                        writeSmart(type.id)
                    }
                    writeColumnFields(columnTypes, columnValues!![columnId])
                }
                writeByte(255)
            }
        }
        if (definition.tableId != -1) {
            writeByte(4)
            writeVarInt(definition.tableId)
        }
        writeByte(0)
    }

    override fun createDefinition() = DBRowType()

    private fun Reader.readVarInt2(): Int {
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

    private fun Writer.writeVarInt(var1: Int) {
        if (var1 and -128 != 0) {
            if (var1 and -16384 != 0) {
                if (var1 and -2097152 != 0) {
                    if (var1 and -268435456 != 0) {
                        writeByte(var1 ushr 28 or 128)
                    }
                    writeByte(var1 ushr 21 or 128)
                }
                writeByte(var1 ushr 14 or 128)
            }
            writeByte(var1 ushr 7 or 128)
        }
        writeByte(var1 and 127)
    }
}
