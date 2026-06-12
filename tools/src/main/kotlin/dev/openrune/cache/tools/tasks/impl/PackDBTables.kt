package dev.openrune.cache.tools.tasks.impl

import com.displee.cache.CacheLibrary
import dev.openrune.cache.CacheDelegate
import dev.openrune.cache.DBROW
import dev.openrune.cache.DBTABLE
import dev.openrune.cache.DBTABLEINDEX
import dev.openrune.cache.gameval.GameValElement
import dev.openrune.cache.gameval.impl.Table
import dev.openrune.cache.tools.CacheTool
import dev.openrune.cache.tools.tasks.CacheTask
import dev.openrune.cache.util.progress
import dev.openrune.definition.GameValGroupTypes
import dev.openrune.definition.codec.DBRowCodec
import dev.openrune.definition.codec.DBTableCodec
import dev.openrune.definition.dbtables.DBTable
import dev.openrune.definition.dbtables.DBTableToml
import dev.openrune.definition.type.DBRowType
import dev.openrune.definition.type.DBTableType
import dev.openrune.definition.util.toArray
import dev.openrune.filesystem.Cache
import io.netty.buffer.Unpooled
import java.io.File
import kotlin.collections.forEach
import kotlin.collections.iterator

/**
 * Packs custom DB tables and rows into the cache.
 *
 * Use the DSL (`dbTable { … }`) or TOML files ([DBTableToml]):
 *
 * ```
 * +PackDBTables(myTable)
 * +PackDBTables(File("data/dbtables"))   // directory of *.toml
 * +PackDBTables.fromToml(File("shop.toml"))
 * ```
 */
public class PackDBTables constructor(private val tables: List<DBTable>) : CacheTask() {

    public constructor(vararg tables: DBTable) : this(tables.toList())

    /** Loads every `*.toml` table definition in [directory]. */
    public constructor(directory: File) : this(DBTableToml.loadDirectory(directory))

    companion object {
        fun fromToml(file: File): PackDBTables = PackDBTables(DBTableToml.load(file))

        fun fromTomlDirectory(directory: File): PackDBTables = PackDBTables(DBTableToml.loadDirectory(directory))
    }

    private val rowCodec = DBRowCodec()
    private val tableCodec = DBTableCodec()

    override fun init(cache: Cache) {
        val library = (cache as CacheDelegate).library


        val dbtableArchive = library.index(2).archive(DBTABLE) ?: return
        val dbrowArchive = library.index(2).archive(DBROW) ?: return

        val tablesToPack = tables.filter { table -> table.serverOnly == serverPass }
        if (tablesToPack.isEmpty()) return
        val progress = progress("Packing DB Tables", tablesToPack.size)

        tablesToPack.forEach { table ->
            try {
                val tableType = table.toDbTableType()
                val rowTypes = table.toDbRowTypes()


                val writer = Unpooled.buffer(4096)
                with(tableCodec) { writer.encode(tableType) }
                dbtableArchive.add(tableType.id, writer.toArray())

                CacheTool.Companion.addGameValMapping(
                    GameValGroupTypes.TABLETYPES,
                    Table(
                        table.rscmName ?: "table_${table.tableId}",
                        table.tableId,
                        table.columns.map { Table.Column(it.value.rscmName ?: "col_${it.key}", it.key) }
                    )
                )

                rowTypes.forEach { rowType ->
                    val writer1 = Unpooled.buffer(4096)
                    with(rowCodec) { writer1.encode(rowType) }

                    val rowRscmName = rowType.rscmName ?: "row_${rowType.id}"

                    CacheTool.Companion.addGameValMapping(
                        GameValGroupTypes.ROWTYPES,
                        GameValElement(rowRscmName, rowType.id)
                    )
                    dbrowArchive.add(rowType.id, writer1.toArray())
                }

                packDbTableIndex(library, table)
            }catch (e : Exception) {
                e.printStackTrace()
            }

            progress.step()
        }
        progress.close()
    }

    private fun DBTable.toDbTableType(): DBTableType {
        val dbTable = DBTableType(this.tableId)
        dbTable.columns.putAll(this.columns)
        return dbTable
    }

    private fun DBTable.toDbRowTypes(): List<DBRowType> {
        val dbRows = mutableListOf<DBRowType>()
        this.rows.forEach { dbRow ->
            val row = DBRowType(dbRow.rowId, dbRow.rscmName)
            row.tableId = this.tableId
            val maxColumnId = dbRow.columns.keys.maxOrNull() ?: -1
            if (maxColumnId >= 0) {
                row.ensureColumnStorage(maxColumnId + 1)
                for ((columnId, values) in dbRow.columns) {
                    val columnDef = this.columns[columnId]

                    if (columnDef !=  null) {
                        row.field5306!![columnId] = IntArray(columnDef.types.size) { i -> columnDef.types[i].id }
                        row.columnTypes!![columnId] = Array(values.size) { i -> values[i] }
                    }

                }
            }
            dbRows.add(row)
        }
        return dbRows
    }

    private fun packDbTableIndex(library: CacheLibrary, table: DBTable) {
        if (!library.exists(DBTABLEINDEX)) {
            System.err.println(
                "[PackDBTables] Skipping dbtable index pack: index $DBTABLEINDEX not in cache (table id=${table.tableId})",
            )
            return
        }
        val maxCol = table.columns.keys.maxOrNull() ?: return
        if (maxCol < 0) return

        val idx = library.index(DBTABLEINDEX)
        if (!idx.contains(table.tableId)) {
            idx.add(table.tableId)
        }
        idx.archive(table.tableId)
        val arc = idx.archive(table.tableId) ?: return

        val empty = DBTableIndexPacker.emptyIndexPayload()
        arc.add(0, DBTableIndexPacker.encodeMasterRowIndex(table), overwrite = true)
        for (col in 0..maxCol) {
            val blob = DBTableIndexPacker.encodeColumnIndex(table, col) ?: empty
            arc.add(col + 1, blob, overwrite = true)
        }
    }
}