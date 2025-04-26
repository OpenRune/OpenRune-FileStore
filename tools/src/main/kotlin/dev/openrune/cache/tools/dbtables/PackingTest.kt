package dev.openrune.cache.tools.dbtables

import com.displee.cache.CacheLibrary
import dev.openrune.cache.DBROW
import dev.openrune.cache.DBTABLE
import dev.openrune.definition.codec.DBRowCodec
import dev.openrune.definition.codec.DBTableCodec
import dev.openrune.definition.util.Type
import dev.openrune.definition.util.toArray
import io.netty.buffer.Unpooled

object PackingTest {
    private val rowCodec = DBRowCodec()
    private val tableCodec = DBTableCodec()

    @JvmStatic
    fun main(args: Array<String>) {
        val tables = listOf(table)
        val library = CacheLibrary("C:\\Users\\Advo\\Downloads\\cache-oldschool-live-en-b227-2024-12-10-12-15-06-openrs2#1984\\cache")

        val dbtable_archive = library.index(2).archive(DBTABLE) ?: return
        val dbrow_archive = library.index(2).archive(DBROW) ?: return
        tables.forEach { table ->
            val tableType = table.toDbTableType()
            val rowTypes = table.toDbRowTypes()


            val writer = Unpooled.buffer(4096)
            with(tableCodec) { writer.encode(tableType) }
            dbtable_archive.add(tableType.id, writer.toArray())

            rowTypes.forEach {
                val writer1 = Unpooled.buffer(4096)
                with(rowCodec) { writer1.encode(it) }
                dbtable_archive.add(it.id, writer1.toArray())
            }
        }

    }

    val table = dbTable(2) {
        column(0, arrayOf(Type.STRING))
        column(1, arrayOf(Type.GRAPHIC, Type.INT, Type.INT))
        column(2, arrayOf(Type.INT, Type.INT), arrayOf(0, 0))
        column(3, arrayOf(Type.COMPONENT))

        row(3784) {
            column(0, arrayOf("Quests"))
            column(1, arrayOf(835, 18, 18))
            column(3, arrayOf(24379400))
        }
        row(3785) {
            column(0, arrayOf("Skills"))
            column(1, arrayOf(3387, 18, 18))
            column(3, arrayOf(24379401))
        }
        row(3786) {
            column(0, arrayOf("Utility"))
            column(1, arrayOf(3390, 18, 18))
            column(3, arrayOf(24379402))
        }
        row(3787) {
            column(0, arrayOf("Other"))
            column(1, arrayOf(1439, 16, 16))
            column(3, arrayOf(24379403))
        }
    }
}