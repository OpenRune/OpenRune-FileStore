package dev.openrune.cache.tools.tasks.impl

import dev.openrune.definition.codec.DBTableIndexCodec
import dev.openrune.definition.dbtables.DBTable
import dev.openrune.definition.type.DBTableIndexColumn
import dev.openrune.definition.type.DBTableIndexKey
import dev.openrune.definition.type.DBTableIndexType
import dev.openrune.definition.util.BaseVarType
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled

/**
 * Builds db-table index blobs (index 21) for [DBTableIndexCodec].
 *
 * Layout per table archive (same as RuneLite DBTableIndexManager):
 * - **File 0** — master index (`columnId` -1): key **0** or **-1** ([BaseVarType.INTEGER]) → all row ids.
 * - **File columnId+1** — index for logical column `columnId` (0 .. maxColumnId).
 */
object DBTableIndexPacker {

    private val codec = DBTableIndexCodec()

    /**
     * Master index: single INTEGER bucket with sentinel key **0** (or -1) mapping to every packed row id (sorted).
     */
    fun encodeMasterRowIndex(table: DBTable): ByteArray {
        val rowIds = table.rows.map { it.rowId }.sorted()
        val column = DBTableIndexColumn(
            BaseVarType.INTEGER,
            mapOf(DBTableIndexKey.IntKey(0) to rowIds),
        )
        val def = DBTableIndexType(-1, mutableListOf(column))
        val buf = Unpooled.buffer(64)
        with(codec) { buf.encode(def) }
        return buf.toByteArray()
    }

    fun emptyIndexPayload(): ByteArray {
        val buf = Unpooled.buffer(8)
        with(codec) { buf.encode(DBTableIndexType()) }
        return buf.toByteArray()
    }

    /**
     * @return encoded index for this column, or `null` if the column cannot be indexed (e.g. ARRAY).
     */
    fun encodeColumnIndex(table: DBTable, columnId: Int): ByteArray? {
        val colDef = table.columns[columnId] ?: return null
        val firstLiteral = colDef.types.firstOrNull() ?: return null
        val base = firstLiteral.baseType
        if (base == BaseVarType.ARRAY) return null

        val map = LinkedHashMap<DBTableIndexKey, MutableList<Int>>()
        for (row in table.rows) {
            val cells = row.columns[columnId] ?: continue
            if (cells.isEmpty()) continue
            val key = cellToIndexKey(cells[0], base) ?: return null
            map.getOrPut(key) { mutableListOf() }.add(row.rowId)
        }
        for (e in map) {
            e.value.sort()
        }
        val column = DBTableIndexColumn(
            base,
            map.mapValues { it.value.toList() },
        )
        val def = DBTableIndexType(-1, mutableListOf(column))
        val buf = Unpooled.buffer(256)
        with(codec) { buf.encode(def) }
        return buf.toByteArray()
    }

    private fun cellToIndexKey(value: Any, base: BaseVarType): DBTableIndexKey? {
        return when (base) {
            BaseVarType.INTEGER -> when (value) {
                is Int -> DBTableIndexKey.IntKey(value)
                is Boolean -> DBTableIndexKey.IntKey(if (value) 1 else 0)
                is Number -> DBTableIndexKey.IntKey(value.toInt())
                else -> null
            }
            BaseVarType.LONG -> when (value) {
                is Long -> DBTableIndexKey.LongKey(value)
                is Number -> DBTableIndexKey.LongKey(value.toLong())
                else -> null
            }
            BaseVarType.STRING -> when (value) {
                is String -> DBTableIndexKey.StringKey(value)
                else -> null
            }
            BaseVarType.ARRAY -> null
        }
    }

    private fun ByteBuf.toByteArray(): ByteArray =
        ByteArray(writerIndex()).also { getBytes(0, it) }
}
