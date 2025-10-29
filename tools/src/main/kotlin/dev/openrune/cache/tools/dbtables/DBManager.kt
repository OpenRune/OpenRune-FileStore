package dev.openrune.cache.tools.dbtables

import com.displee.cache.CacheLibrary
import dev.openrune.cache.DBROW
import dev.openrune.cache.DBTABLE
import dev.openrune.definition.codec.DBRowCodec
import dev.openrune.definition.codec.DBTableCodec
import dev.openrune.definition.dbtables.DBRow
import dev.openrune.definition.dbtables.DBTable
import dev.openrune.definition.dbtables.FullDBRow
import dev.openrune.definition.dbtables.toDatabaseTable
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


    fun <T> toDatabaseTable(tableId: Int, constructor: (FullDBRow) -> T): List<T> {
        val table = getTable(tableId) ?: return emptyList()
        val rows = rows.values.mapNotNull { row ->
            if (row.tableId != table.id) return@mapNotNull null
            row
        }
        return toDatabaseTable(table, rows, constructor)
    }
}
