package dev.openrune.cache.tools.dbtables

import com.displee.cache.CacheLibrary
import dev.openrune.cache.DBROW
import dev.openrune.cache.DBTABLE
import dev.openrune.definition.codec.DBRowCodec
import dev.openrune.definition.codec.DBTableCodec
import dev.openrune.definition.type.DBColumnType
import dev.openrune.definition.type.DBRowType
import dev.openrune.definition.type.DBTableType
import java.util.*
import kotlin.collections.HashMap

class DBManager(private val store: CacheLibrary) {
    private val tables: MutableMap<Int, DBTableType> = HashMap()
    private val rows: MutableMap<Int, DBRowType> = HashMap()
    private val rowCodec = DBRowCodec()
    private val tableCodec = DBTableCodec()

    fun load() {
        val dbtable_archive = store.index(2).archive(DBTABLE) ?: return
        val dbrow_archive = store.index(2).archive(DBROW) ?: return

        for (f in dbtable_archive.files()) {
            val table = tableCodec.loadData(f.id, f.data)
            tables[f.id] = table
        }

        for (f in dbrow_archive.files()) {
            val row = rowCodec.loadData(f.id, f.data)
            rows[f.id] = row
        }
    }

    fun getTables(): Collection<DBTableType> {
        return Collections.unmodifiableCollection(tables.values)
    }

    fun getTable(tableId: Int): DBTableType? {
        return tables[tableId]
    }

    fun getRows(): Collection<DBRowType> {
        return Collections.unmodifiableCollection(rows.values)
    }

    fun getRow(rowId: Int): DBRowType? {
        return rows[rowId]
    }

    fun getTableWithRows(tableId: Int): DBTable? {
        val table = tables[tableId] ?: return null

        val streamlinedRows = rows.values.mapNotNull { row ->
            if (row.tableId != tableId) return@mapNotNull null

            val filteredColumns = row.columns.mapNotNull { (columnId, column) ->
                column.values?.let { columnId to it }
            }.toMap()

            if (filteredColumns.isEmpty()) return@mapNotNull null

            DBRow(rowId = row.id, columns = filteredColumns)
        }

        return DBTable(table.id, table.columns, streamlinedRows)
    }

    fun convertToDBTableType(dbTableWithRows: DBTable): DBTableType {
        return DBTableType(dbTableWithRows.tableId).apply {
            columns.putAll(dbTableWithRows.columns)
        }
    }

    fun convertToDBRowType(dbTableWithRows: DBTable): List<DBRowType> {
        return dbTableWithRows.rows.map { row ->
            DBRowType(row.rowId).apply {
                tableId = dbTableWithRows.tableId
                columns.putAll(row.columns.mapValues { (columnId, values) ->
                    DBColumnType(dbTableWithRows.columns[columnId]?.types ?: emptyArray(), values)
                })
            }
        }
    }

    fun <T> toDatabaseTable(tableId: Int, constructor: (FullDBRow) -> T): List<T> {
        val table = getTable(tableId) ?: return emptyList()
        val rows = rows.values.mapNotNull { row ->
            if (row.tableId != table.id) return@mapNotNull null
            row
        }
        return toDatabaseTable(table, rows, constructor)
    }

    companion object {
       
        inline fun <T> toDatabaseTable(table: DBTableType, rows: List<DBRowType>, constructor: (FullDBRow) -> T): List<T> {
            val result = mutableListOf<T>()
            for (row in rows) {
                val fullColumns = mutableMapOf<Int, Array<Any>>()

                for ((colId, columnDef) in table.columns) {
                    val value = row.columns[colId]
                    if (value?.values != null) {
                        fullColumns[colId] = value.values!!
                    } else {
                        val default = columnDef.values
                        if (default != null) {
                            fullColumns[colId] = default
                        } else {
                            fullColumns[colId] = emptyArray<Any>()
                        }
                    }
                }

                result.add(constructor(FullDBRow(row.id, fullColumns)))
            }
            return result
        }
    }
}