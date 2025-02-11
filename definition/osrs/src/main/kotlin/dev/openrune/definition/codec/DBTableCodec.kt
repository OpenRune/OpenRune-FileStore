package dev.openrune.definition.codec

import dev.openrune.definition.util.readSmart
import dev.openrune.definition.util.readString
import dev.openrune.definition.util.writeSmart
import dev.openrune.definition.util.writeString
import dev.openrune.definition.DefinitionCodec
import dev.openrune.definition.type.DBColumnType
import dev.openrune.definition.type.DBTableType
import dev.openrune.definition.util.Type
import io.netty.buffer.ByteBuf

class DBTableCodec : DefinitionCodec<DBTableType> {
    override fun DBTableType.read(opcode: Int, buffer: ByteBuf) {
        when (opcode) {
            1 -> {
                val numColumns = buffer.readUnsignedByte().toInt()
                initialize(numColumns)
                var setting = buffer.readUnsignedByte().toInt()
                while (setting != 255) {
                    val columnId = setting and 0x7F
                    val hasDefault = setting and 0x80 != 0
                    val columnTypes = Array(buffer.readUnsignedByte().toInt()) {
                        Type.byID(buffer.readSmart())
                    }
                    val defaultValues = if (hasDefault) decodeColumnFields(buffer, columnTypes) else null
                    columns[columnId] = DBColumnType(columnId, columnTypes, defaultValues)

                    setting = buffer.readUnsignedByte().toInt()
                }
            }
        }
    }

    override fun ByteBuf.encode(definition: DBTableType) {
        if (definition.columns.isEmpty()) {
            writeByte(0)
            return
        }
        writeByte(1)
        writeByte(definition.columns.size)
        definition.columns.forEach { column ->
            if(column == null)
                return@forEach
            val hasDefault = column.values != null
            var setting = column.id
            if (hasDefault) {
                setting = setting or 0x80
            }
            writeByte(setting)
            writeByte(column.types.size)
            for (type in column.types) {
                writeSmart(type.id)
            }
            if (hasDefault) {
                writeColumnFields(column.types, column.values)
            }
        }

        writeByte(255)
        writeByte(0)
    }

    override fun createDefinition() = DBTableType()
}

fun ByteBuf.writeColumnFields(types: Array<Type>, values: Array<Any>?) {
    val fieldCount = values!!.size / types.size
    writeSmart(fieldCount)
    for (fieldIndex in 0 until fieldCount) {
        for (typeIndex in types.indices) {
            val type = types[typeIndex]
            val valuesIndex = fieldIndex * types.size + typeIndex
            if (type == Type.STRING) {
                writeString(values[valuesIndex] as String?)
            } else {
                writeInt((values[valuesIndex] as Int?)!!)
            }
        }
    }
}

fun decodeColumnFields(buffer: ByteBuf, types: Array<Type>): Array<Any> {
    val fieldCount = buffer.readSmart()
    val values = arrayOfNulls<Any>(fieldCount * types.size)
    for (fieldIndex in 0 until fieldCount) {
        for (typeIndex in types.indices) {
            val type = types[typeIndex]
            val valuesIndex = fieldIndex * types.size + typeIndex
            values[valuesIndex] = when (type) {
                Type.STRING -> buffer.readString()
                else -> buffer.readInt()
            }
        }
    }
    return values.requireNoNulls()
}