package dev.openrune.cache.tools.dbtables

import dev.openrune.cache.CacheDelegate
import dev.openrune.cache.DBROW
import dev.openrune.cache.DBTABLE
import dev.openrune.cache.tools.CacheTool
import dev.openrune.cache.tools.tasks.CacheTask
import dev.openrune.cache.util.progress
import dev.openrune.definition.Js5GameValGroup
import dev.openrune.definition.codec.DBRowCodec
import dev.openrune.definition.codec.DBTableCodec
import dev.openrune.definition.dbtables.DBTable
import dev.openrune.definition.dbtables.columnNames
import dev.openrune.definition.dbtables.rowNames
import dev.openrune.definition.dbtables.tableNames
import dev.openrune.definition.dbtables.toDbRowTypes
import dev.openrune.definition.dbtables.toDbTableType
import dev.openrune.definition.util.toArray
import dev.openrune.filesystem.Cache
import io.netty.buffer.Unpooled

class PackDBTables(private val tables : List<DBTable>) : CacheTask() {

    private val rowCodec = DBRowCodec()
    private val tableCodec = DBTableCodec()

    override fun init(cache: Cache) {
        val library = (cache as CacheDelegate).library
        val progress = progress("Packing DB Tables", tables.size)

        val dbtableArchive = library.index(2).archive(DBTABLE) ?: return
        val dbrowArchive = library.index(2).archive(DBROW) ?: return

        tables.forEach { table ->
            val tableType = table.toDbTableType()
            val rowTypes = table.toDbRowTypes()


            val writer = Unpooled.buffer(4096)
            with(tableCodec) { writer.encode(tableType) }
            dbtableArchive.add(tableType.id, writer.toArray())

            val columnNamesList = table.columns.keys.map { columnNames[it] ?: "" }

            CacheTool.addGameValMapping(
                Js5GameValGroup.TABLETYPES,
                "${tableNames[table.tableId] ?: ""}:[${columnNamesList.joinToString(",")}]",
                table.tableId
            )

            rowTypes.forEach {
                val writer1 = Unpooled.buffer(4096)
                with(rowCodec) { writer1.encode(it) }
                CacheTool.addGameValMapping(Js5GameValGroup.ROWTYPES, rowNames[it.id] ?: "",it.id)

                dbrowArchive.add(it.id, writer1.toArray())
            }

            progress.step()
        }
    }


}