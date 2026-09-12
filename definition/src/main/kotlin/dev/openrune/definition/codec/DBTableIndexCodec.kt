package dev.openrune.definition.codec

import dev.openrune.definition.DefinitionCodec
import dev.openrune.definition.type.DBTableIndexColumn
import dev.openrune.definition.type.DBTableIndexKey
import dev.openrune.definition.type.DBTableIndexType
import dev.openrune.definition.type.builders.DBTableIndexTypeBuilder
import dev.openrune.definition.util.BaseVarType
import dev.openrune.definition.util.readString
import dev.openrune.definition.util.readVarInt
import dev.openrune.definition.util.writeString
import dev.openrune.definition.util.writeVarInt
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled

class DBTableIndexCodec : DefinitionCodec<DBTableIndexType> {

    override fun DBTableIndexType.read(opcode: Int, buffer: ByteBuf) =
        error("DBTableIndexType is immutable; decoding goes through DBTableIndexTypeBuilder")

    override fun loadData(id: Int, data: ByteBuf?): DBTableIndexType = decode(id, data)

    override fun loadData(id: Int, data: ByteArray?): DBTableIndexType =
        decode(id, data?.takeIf { it.isNotEmpty() }?.let { Unpooled.wrappedBuffer(it) })

    /** The blob is one fixed layout, not an opcode stream, so it is read in a single pass. */
    private fun decode(id: Int, data: ByteBuf?): DBTableIndexType {
        val builder = DBTableIndexTypeBuilder(id)
        if (data != null && data.readableBytes() > 0) {
            try {
                builder.read(data)
            } catch (e: Exception) {
                throw IllegalStateException("Unable to decode DBTableIndexType [$id]", e)
            }
        }
        return builder.build()
    }

    private fun DBTableIndexTypeBuilder.read(buffer: ByteBuf) {
        columns.clear()
        val tupleSize = buffer.readVarInt()
        repeat(tupleSize) {
            val baseType = BaseVarType.entries[buffer.readUnsignedByte().toInt()]
            var valueCount = buffer.readVarInt()
            val valueToRows = LinkedHashMap<DBTableIndexKey, List<Int>>(valueCount)
            while (valueCount-- > 0) {
                val key = decodeKey(baseType, buffer)
                var rowCount = buffer.readVarInt()
                val rowIds = ArrayList<Int>(rowCount)
                while (rowCount-- > 0) {
                    rowIds.add(buffer.readVarInt())
                }
                valueToRows[key] = rowIds
            }
            columns.add(DBTableIndexColumn(baseType, valueToRows))
        }
    }

    override fun ByteBuf.encode(definition: DBTableIndexType) {
        writeVarInt(definition.columns.size)
        for (column in definition.columns) {
            writeByte(column.valueType.ordinal)
            writeVarInt(column.valueToRowIds.size)
            for ((key, rows) in column.valueToRowIds) {
                encodeKey(column.valueType, key)
                writeVarInt(rows.size)
                for (row in rows) {
                    writeVarInt(row)
                }
            }
        }
    }

    override fun createDefinition() = DBTableIndexType()

    private fun decodeKey(baseType: BaseVarType, buffer: ByteBuf): DBTableIndexKey {
        return when (baseType) {
            BaseVarType.INTEGER -> DBTableIndexKey.IntKey(buffer.readInt())
            BaseVarType.LONG -> DBTableIndexKey.LongKey(buffer.readLong())
            BaseVarType.STRING -> DBTableIndexKey.StringKey(buffer.readString())
            BaseVarType.ARRAY -> error("DB table index column type ARRAY is not supported")
        }
    }

    private fun ByteBuf.encodeKey(baseType: BaseVarType, key: DBTableIndexKey) {
        when (baseType) {
            BaseVarType.INTEGER -> writeInt((key as DBTableIndexKey.IntKey).value)
            BaseVarType.LONG -> writeLong((key as DBTableIndexKey.LongKey).value)
            BaseVarType.STRING -> writeString((key as DBTableIndexKey.StringKey).value)
            BaseVarType.ARRAY -> error("DB table index column type ARRAY is not supported")
        }
    }
}
