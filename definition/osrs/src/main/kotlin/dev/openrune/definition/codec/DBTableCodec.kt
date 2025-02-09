package dev.openrune.definition.codec

import dev.openrune.buffer.*
import io.netty.buffer.ByteBuf
import dev.openrune.buffer.Writer
import dev.openrune.buffer.readSmartRD
import dev.openrune.buffer.readStringRD
import dev.openrune.definition.DefinitionCodec
import dev.openrune.definition.type.DBTableType
import dev.openrune.definition.util.Type

class DBTableCodec : DefinitionCodec<DBTableType> {
    override fun DBTableType.read(opcode: Int, buffer: ByteBuf) {
        when (opcode) {
            1 -> {
                val numColumns = buffer.readUnsignedByteRD()
                val types = arrayOfNulls<Array<Type>>(numColumns)
                var defaultValues: Array<Array<Any?>?>? = null
                var setting = buffer.readUnsignedByteRD()
                while (setting != 255) {
                    val columnId = setting and 0x7F
                    val hasDefault = setting and 0x80 != 0
                    val columnTypes = Array(buffer.readUnsignedByteRD()) {
                        Type.byID(buffer.readSmartRD())!!
                    }
                    types[columnId] = columnTypes
                    if (hasDefault) {
                        if (defaultValues == null) {
                            defaultValues = arrayOfNulls<Array<Any?>?>(types.size)
                        }
                        defaultValues[columnId] = decodeColumnFields(buffer, columnTypes)
                    }
                    setting = buffer.readUnsignedByteRD()
                }
                this.types = types
                this.defaultColumnValues = defaultValues
            }
        }
    }

    override fun Writer.encode(definition: DBTableType) {
        val types = definition.types
        val defaultValues: Array<Array<Any?>?>? = definition.defaultColumnValues
        if (types == null) {
            writeByte(0)
            return
        }
        writeByte(1)
        writeByte(types.size)
        types.indices.forEach { i ->
            val columnTypes = types[i] ?: return@forEach
            val hasDefault = defaultValues != null && defaultValues[i] != null
            var setting = i
            if (hasDefault) {
                setting = setting or 0x80
            }
            writeByte(setting)
            writeByte(columnTypes.size)
            for (type in columnTypes) {
                writeSmart(type.id)
            }
            if (hasDefault) {
                writeColumnFields(columnTypes, defaultValues!![i])
            }
        }
        writeByte(255)
        writeByte(0)
    }

    override fun createDefinition() = DBTableType()
}

fun Writer.writeColumnFields(types: Array<Type>, values: Array<Any?>?) {
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

fun decodeColumnFields(buffer: ByteBuf, types: Array<Type>): Array<Any?> {
    val fieldCount = buffer.readSmartRD()
    val values = arrayOfNulls<Any>(fieldCount * types.size)
    for (fieldIndex in 0 until fieldCount) {
        for (typeIndex in types.indices) {
            val type = types[typeIndex]
            val valuesIndex = fieldIndex * types.size + typeIndex
            values[valuesIndex] = when (type) {
                Type.STRING -> buffer.readStringRD()
                else -> buffer.readIntRD()
            }
        }
    }
    return values
}