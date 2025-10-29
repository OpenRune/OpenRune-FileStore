package dev.openrune.definition.codec

import dev.openrune.definition.util.readSmart
import dev.openrune.definition.util.readString
import dev.openrune.definition.util.writeSmart
import dev.openrune.definition.util.writeString
import dev.openrune.definition.DefinitionCodec
import dev.openrune.definition.type.DBColumnType
import dev.openrune.definition.type.DBTableType
import dev.openrune.definition.util.BaseVarType
import dev.openrune.definition.util.VarType
import io.netty.buffer.ByteBuf

class DBTableCodec : DefinitionCodec<DBTableType> {
    override fun DBTableType.read(opcode: Int, buffer: ByteBuf) {
        when (opcode) {
            1 -> {
                //this is the number of columns. Potentially in the future we can set the size of the map to this.
                buffer.readUnsignedByte().toInt()
                var setting = buffer.readUnsignedByte().toInt()
                while (setting != 255) {
                    val columnId = setting and 0x7F
                    val hasDefault = setting and 0x80 != 0
                    val columnTypes = Array(buffer.readUnsignedByte().toInt()) {
                        VarType.byID(buffer.readSmart())
                    }
                    val defaultValues = if (hasDefault) decodeColumnFields(buffer, columnTypes) else null
                    columns[columnId] = DBColumnType(columnTypes, defaultValues)

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
        definition.columns.entries.forEach { entry ->
            val column = entry.value
            val hasDefault = column.values != null
            var setting = entry.key
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

fun ByteBuf.writeColumnFields(types: Array<VarType>, values: Array<Any>?) {
    requireNotNull(values) { "Values array cannot be null" }

    val fieldCount = values.size / types.size
    writeSmart(fieldCount)

    for (fieldIndex in 0 until fieldCount) {
        for (typeIndex in types.indices) {
            val type = types[typeIndex]
            val valueIndex = fieldIndex * types.size + typeIndex
            val value = values[valueIndex]

            when (type.baseType) {
                BaseVarType.INTEGER -> {
                    val intValue = when (type) {
                        VarType.BOOLEAN -> when (value) {
                            is Boolean -> if (value) 1 else 0
                            is Number -> value.toInt()
                            else -> error("Expected Boolean or Number for BOOLEAN type, got ${value.javaClass.simpleName}")
                        }
                        else -> (value as? Number)?.toInt()
                            ?: error("Expected Number for type ${type.name}, got ${value.javaClass.simpleName}")
                    }
                    writeInt(intValue)
                }
                BaseVarType.LONG -> writeLong((value as? Number)?.toLong()
                    ?: error("Expected Number for type ${type.name}, got ${value.javaClass.simpleName}"))
                BaseVarType.STRING -> writeString(value as? String)
                null -> error("Type ${type.name} has no base type defined")
            }
        }
    }
}

fun decodeColumnFields(buffer: ByteBuf, types: Array<VarType>): Array<Any> {
    val fieldCount = buffer.readSmart()
    val values = arrayOfNulls<Any>(fieldCount * types.size)
    for (fieldIndex in 0 until fieldCount) {
        for (typeIndex in types.indices) {
            val type = types[typeIndex]
            val valuesIndex = fieldIndex * types.size + typeIndex
            values[valuesIndex] = when (type) {
                VarType.STRING -> buffer.readString()
                else -> buffer.readInt()
            }
        }
    }
    return values.requireNoNulls()
}