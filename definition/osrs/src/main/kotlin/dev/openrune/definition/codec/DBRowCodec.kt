package dev.openrune.definition.codec

import dev.openrune.definition.util.readSmart
import dev.openrune.definition.util.writeSmart
import dev.openrune.definition.DefinitionCodec
import dev.openrune.definition.type.DBColumnType
import dev.openrune.definition.type.DBRowType
import dev.openrune.definition.util.VarType
import io.netty.buffer.ByteBuf

class DBRowCodec : DefinitionCodec<DBRowType> {
    override fun DBRowType.read(opcode: Int, buffer: ByteBuf) {
        when (opcode) {
            3 -> {
                //this is the number of columns. Potentially in the future we can set the size of the map to this.
                buffer.readUnsignedByte().toInt()
                var columnId = buffer.readUnsignedByte().toInt()
                while (columnId != 255) {
                    val columnTypes = Array(buffer.readUnsignedByte().toInt()) {
                        VarType.byID(buffer.readSmart())
                    }
                    val columnValues = decodeColumnFields(buffer, columnTypes)
                    columns[columnId] = DBColumnType(columnTypes, columnValues)
                    columnId = buffer.readUnsignedByte().toInt()
                }
            }

            4 -> this.tableId = buffer.readVarInt2()
        }
    }

    override fun ByteBuf.encode(definition: DBRowType) {
        if (definition.columns.isNotEmpty()) {
            writeByte(3)
            writeByte(definition.columns.size)
            definition.columns.entries.forEach { entry ->
                val column = entry.value

                writeByte(entry.key)
                writeByte(column.types.size)
                for (type in column.types) {
                    writeSmart(type.id)
                }
                writeColumnFields(column.types, column.values)
            }
            writeByte(255)
        }
        if (definition.tableId != -1) {
            writeByte(4)
            writeVarInt(definition.tableId)
        }
        writeByte(0)
    }

    override fun createDefinition() = DBRowType()

    private fun ByteBuf.readVarInt2(): Int {
        var value = 0
        var bits = 0
        var read: Int
        do {
            read = readUnsignedByte().toInt()
            value = value or (read and 0x7F shl bits)
            bits += 7
        } while (read > 127)
        return value
    }

    fun ByteBuf.writeVarInt(value: Int): ByteBuf {
        var v = value
        while ((v and 0xFFFFFF80.toInt()) != 0) {
            writeByte((v and 0x7F) or 0x80)
            v = v ushr 7
        }
        writeByte(v and 0x7F)
        return this
    }
}
