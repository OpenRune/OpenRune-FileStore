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
import dev.openrune.definition.util.readColumnValues
import dev.openrune.definition.util.readUnsignedShortSmart
import dev.openrune.definition.util.writeColumnValues
import dev.openrune.definition.util.writeUnsignedShortSmart
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
                    val defaultValues = if (hasDefault) buffer.readColumnValues(columnTypes) else null
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
                writeColumnValues(column.values, column.types)
            }
        }

        writeByte(255)
        writeByte(0)
    }

    override fun createDefinition() = DBTableType()
}