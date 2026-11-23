package dev.openrune.definition.codec

import dev.openrune.definition.type.Rs2DbColumn
import dev.openrune.definition.type.Rs2DbTableType
import dev.openrune.definition.util.readColumnValues
import dev.openrune.definition.util.readSmart
import dev.openrune.definition.util.writeColumnValues
import dev.openrune.definition.util.writeSmart
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled

class Rs2DbTableCodec : Rs2OrderedCodec<Rs2DbTableType> {

    override fun Rs2DbTableType.read(opcode: Int, buffer: ByteBuf) {
        when (opcode) {
            1 -> {
                sizePrefixed = false
                columnCount = buffer.readUnsignedByte().toInt()
                while (true) {
                    val value = buffer.readUnsignedByte().toInt()
                    if (value == 255) break
                    val column = value and 127
                    val hasDefault = (value and 128) != 0
                    val tupleLength = buffer.readUnsignedByte().toInt()
                    val types = MutableList(tupleLength) { Rs2VarLiteral.byID(buffer.readSmart()) }
                    val values = if (hasDefault) {
                        buffer.readColumnValues(types.toTypedArray()).toMutableList()
                    } else {
                        mutableListOf()
                    }
                    columns += Rs2DbColumn(column, types, values, hasDefault)
                }
            }

            2 -> {
                sizePrefixed = true
                buffer.readInt()
                columnCount = buffer.readUnsignedByte().toInt()
                while (true) {
                    val column = buffer.readUnsignedByte().toInt()
                    if (column == 255) break
                    val entry = Rs2DbColumn(column)
                    while (true) {
                        val op = buffer.readUnsignedByte().toInt()
                        if (op == 0) break
                        when (op) {
                            1 -> {
                                val tupleLength = buffer.readUnsignedByte().toInt()
                                entry.types = MutableList(tupleLength) { Rs2VarLiteral.byID(buffer.readSmart()) }
                            }

                            2 -> {
                                entry.hasDefault = true
                                entry.values = buffer.readColumnValues(entry.types.toTypedArray()).toMutableList()
                            }

                            else -> error("Invalid RS3 dbtable column op: $op")
                        }
                    }
                    columns += entry
                }
            }
        }
    }

    override fun ByteBuf.writePayload(opcode: Int, occurrence: Int, definition: Rs2DbTableType) {
        if (opcode == 1) {
            writeByte(definition.columnCount)
            for (column in definition.columns) {
                writeByte(column.column or if (column.hasDefault) 128 else 0)
                writeByte(column.types.size)
                column.types.forEach { writeSmart(it.id) }
                if (column.hasDefault) {
                    writeColumnValues(column.values.toTypedArray(), column.types.toTypedArray())
                }
            }
            writeByte(255)
        } else {
            val body = Unpooled.buffer()
            try {
                body.writeByte(definition.columnCount)
                for (column in definition.columns) {
                    body.writeByte(column.column)
                    if (column.types.isNotEmpty()) {
                        body.writeByte(1)
                        body.writeByte(column.types.size)
                        column.types.forEach { body.writeSmart(it.id) }
                    }
                    if (column.hasDefault) {
                        body.writeByte(2)
                        body.writeColumnValues(column.values.toTypedArray(), column.types.toTypedArray())
                    }
                    body.writeByte(0)
                }
                body.writeByte(255)
                writeInt(body.readableBytes())
                writeBytes(body)
            } finally {
                body.release()
            }
        }
    }

    override fun createDefinition() = Rs2DbTableType()
}
