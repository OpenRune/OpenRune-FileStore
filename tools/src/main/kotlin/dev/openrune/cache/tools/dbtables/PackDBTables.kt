package dev.openrune.cache.tools.dbtables

import dev.openrune.cache.CacheDelegate
import dev.openrune.cache.DBROW
import dev.openrune.cache.DBTABLE
import dev.openrune.cache.gameval.GameValElement
import dev.openrune.cache.gameval.impl.Table
import dev.openrune.cache.tools.CacheTool
import dev.openrune.cache.tools.tasks.CacheTask
import dev.openrune.cache.util.progress
import dev.openrune.definition.GameValGroupTypes
import dev.openrune.definition.codec.DBRowCodec
import dev.openrune.definition.codec.DBTableCodec
import dev.openrune.definition.dbtables.*
import dev.openrune.definition.type.DBColumnType
import dev.openrune.definition.type.DBRowType
import dev.openrune.definition.type.DBTableType
import dev.openrune.definition.util.toArray
import dev.openrune.filesystem.Cache
import io.netty.buffer.Unpooled
import kotlin.collections.set

class PackDBTables(private val tables : List<DBTable>) : CacheTask() {

    private val rowCodec = DBRowCodec()
    private val tableCodec = DBTableCodec()

    override fun init(cache: Cache) {
        val library = (cache as CacheDelegate).library
        val progress = progress("Packing DB Tables", tables.size)

        val dbtableArchive = library.index(2).archive(DBTABLE) ?: return
        val dbrowArchive = library.index(2).archive(DBROW) ?: return

        tables.forEach { table ->
            try {
                val tableType = table.toDbTableType()
                val rowTypes = table.toDbRowTypes()


                val writer = Unpooled.buffer(4096)
                with(tableCodec) { writer.encode(tableType) }
                dbtableArchive.add(tableType.id, writer.toArray())

                CacheTool.addGameValMapping(
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

                    CacheTool.addGameValMapping(GameValGroupTypes.ROWTYPES, GameValElement(rowRscmName, rowType.id))
                    dbrowArchive.add(rowType.id, writer1.toArray())
                }
            }catch (e : Exception) {
                e.printStackTrace()
            }

            progress.step()
        }
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
            for ((columnId, values) in dbRow.columns) {
                val columnDef = this.columns[columnId]
                checkNotNull(columnDef) { "Invalid column $columnId" }
                row.columns[columnId] = DBColumnType(
                    types = columnDef.types,
                    values = values
                )
            }
            dbRows.add(row)
        }
        return dbRows
    }
}