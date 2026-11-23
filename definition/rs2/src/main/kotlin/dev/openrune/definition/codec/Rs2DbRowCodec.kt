package dev.openrune.definition.codec

import dev.openrune.definition.type.Rs2DbColumn
import dev.openrune.definition.type.Rs2DbRowType
import dev.openrune.definition.util.readColumnValues
import dev.openrune.definition.util.readSmart
import dev.openrune.definition.util.readVarInt
import dev.openrune.definition.util.writeColumnValues
import dev.openrune.definition.util.writeSmart
import dev.openrune.definition.util.writeVarInt
import io.netty.buffer.ByteBuf

class Rs2DbRowCodec : Rs2OrderedCodec<Rs2DbRowType> {

    override fun Rs2DbRowType.read(opcode: Int, buffer: ByteBuf) {
        when (opcode) {
            3 -> {
                leading = buffer.readUnsignedByte().toInt()
                while (true) {
                    val column = buffer.readUnsignedByte().toInt()
                    if (column == 255) break
                    val tupleLength = buffer.readUnsignedByte().toInt()
                    val types = MutableList(tupleLength) { Rs2VarLiteral.byID(buffer.readSmart()) }
                    val values = buffer.readColumnValues(types.toTypedArray())
                    columns += Rs2DbColumn(column, types, values.toMutableList())
                }
            }

            4 -> table = buffer.readVarInt()
            5 -> pluginApi = false
        }
    }

    override fun ByteBuf.writePayload(opcode: Int, occurrence: Int, definition: Rs2DbRowType) {
        when (opcode) {
            3 -> {
                writeByte(definition.leading ?: 0)
                for (column in definition.columns) {
                    writeByte(column.column)
                    writeByte(column.types.size)
                    column.types.forEach { writeSmart(it.id) }
                    writeColumnValues(column.values.toTypedArray(), column.types.toTypedArray())
                }
                writeByte(255)
            }

            4 -> writeVarInt(definition.table!!)
            5 -> Unit
        }
    }

    override fun createDefinition() = Rs2DbRowType()
}
