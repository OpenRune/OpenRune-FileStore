package dev.openrune.definition.codec

import dev.openrune.definition.DefinitionCodec
import dev.openrune.definition.type.DBRowType
import dev.openrune.definition.util.CacheVarLiteral
import dev.openrune.definition.util.readDbCell
import dev.openrune.definition.util.readUnsignedShortSmart
import dev.openrune.definition.util.readVarInt
import dev.openrune.definition.util.writeColumnValues
import dev.openrune.definition.util.writeUnsignedShortSmart
import dev.openrune.definition.util.writeVarInt
import io.netty.buffer.ByteBuf

class DBRowCodec : DefinitionCodec<DBRowType> {
    override fun DBRowType.read(opcode: Int, buffer: ByteBuf) {
        when (opcode) {
            3 -> {
                val columnArrayLength = buffer.readUnsignedByte().toInt()
                if (columnTypes == null) {
                    columnTypes = arrayOfNulls(columnArrayLength)
                    field5306 = arrayOfNulls(columnArrayLength)
                }
                var columnId = buffer.readUnsignedByte().toInt()
                while (columnId != 255) {
                    val typeSlotCount = buffer.readUnsignedByte().toInt()
                    val typeIds = IntArray(typeSlotCount) { buffer.readUnsignedShortSmart() }
                    val tupleCount = buffer.readUnsignedShortSmart()
                    val cells = arrayOfNulls<Any>(typeSlotCount * tupleCount)
                    for (tupleIndex in 0 until tupleCount) {
                        for (typeIndex in 0 until typeSlotCount) {
                            val cellIndex = tupleIndex * typeSlotCount + typeIndex
                            val type = CacheVarLiteral.byID(typeIds[typeIndex])
                            cells[cellIndex] = buffer.readDbCell(type)
                        }
                    }
                    columnTypes!![columnId] = cells
                    field5306!![columnId] = typeIds
                    columnId = buffer.readUnsignedByte().toInt()
                }
            }

            4 -> this.tableId = buffer.readVarInt()
        }
    }

    override fun ByteBuf.encode(definition: DBRowType) {
        val cols = definition.columnTypes
        val typeIds = definition.field5306
        if (cols != null && typeIds != null && cols.any { it != null }) {
            writeByte(3)
            writeByte(cols.size)
            for (columnId in cols.indices) {
                val values = cols[columnId] ?: continue
                val ids = typeIds[columnId] ?: continue
                val types = Array(ids.size) { i -> CacheVarLiteral.byID(ids[i]) }
                writeByte(columnId)
                writeByte(types.size)
                for (id in ids) {
                    writeUnsignedShortSmart(id)
                }
                writeColumnValues(values.requireNoNulls(), types)
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
}
