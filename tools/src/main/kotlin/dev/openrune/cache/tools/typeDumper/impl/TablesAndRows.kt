package dev.openrune.cache.tools.typeDumper.impl

import dev.openrune.cache.CacheManager
import dev.openrune.cache.gameval.GameValElement
import dev.openrune.cache.gameval.GameValHandler
import dev.openrune.cache.gameval.GameValHandler.elementAs
import dev.openrune.cache.gameval.impl.Table
import dev.openrune.cache.tools.typeDumper.Language
import dev.openrune.cache.tools.typeDumper.TypeDumper
import dev.openrune.cache.util.Namer
import dev.openrune.cache.util.Namer.Companion.formatForClassName
import dev.openrune.definition.GameValGroupTypes
import dev.openrune.definition.GameValGroupTypes.ROWTYPES
import dev.openrune.definition.GameValGroupTypes.TABLETYPES
import dev.openrune.filesystem.Cache

class TablesAndRows(
    private val typeDumper: TypeDumper,
    private val writeToJava: Boolean
) {

    private val namer = Namer()

    fun writeTablesAndRows(group: GameValGroupTypes, cache: Cache) {
        val tableColumns = mutableMapOf<String, MutableList<Pair<String, String>>>()
        val tableIds = mutableMapOf<String, Int>()
        val tables = GameValHandler.readGameVal(TABLETYPES,cache)
        val rows = GameValHandler.readGameVal(ROWTYPES,cache)

        if (typeDumper.exportSettings.useGameVal!!.combineTablesAndRows) {
            val (components, pathComponents) = typeDumper.generateWriter(group, override = "DBTables")


            tables.forEach {
                val tableData = it.elementAs<Table>()
                val table = tableData!!.name

                tableIds[table] = it.id
                tableData.columns.forEach { col ->
                    tableColumns.getOrPut(table) { mutableListOf() }.add("COL_${col.name}" to col.id.toString())
                }
            }

            tableColumns.forEach { (table, columns) ->

                val tableId = tableIds[table] ?: -1
                val allRows = CacheManager.getRows().filterValues { it.tableId == tableId }.keys
                val attachedRows = rows.filter { it.id in allRows }
                if (typeDumper.language == Language.RSCM) {

                    typeDumper.write(components, "${table}:${tableId}")

                    for ((colName, colId) in columns) {
                        val colKey = namer.name(colName, colId, typeDumper.language, writeToJava)
                        typeDumper.write(components, "${table}.col.${colKey?.replace("COL_","")}:${colId}")
                    }

                    attachedRows.forEach {
                        val rowKey = namer.name(it.name, it.id.toString(), typeDumper.language, writeToJava)
                        typeDumper.write(components, "${table}.row.${rowKey}:${it.id}")
                    }
                } else {
                    typeDumper.write(components, buildClass(table, columns, tableId.toString(), attachedRows).toString())
                }
            }

            typeDumper.endWriter(components, pathComponents)
        } else {
            val dbTables = emptyList<String>().toMutableList()

            tables.forEach {
                tableIds[it.name] = it.id
                val entry = "${it.name}:${it.id}"
                if (entry !in dbTables) {
                    dbTables.add(entry)
                }
            }


            typeDumper.writeGeneralGroupData(TABLETYPES, writeToJava)
            typeDumper.writeGeneralGroupData(ROWTYPES, writeToJava)

        }

    }

    private fun buildClass(className: String, columns: List<Pair<String, String>>, tableId: String, rows: List<GameValElement>): StringBuilder {
        val builder = StringBuilder()
        val classDeclaration = if (typeDumper.language == Language.JAVA) "public static final class" else "object"

        builder.appendLine("$classDeclaration ${formatForClassName(className)} {")
        builder.appendLine()
        namer.used.clear()

        val tableIdField = namer.name("ID", tableId, typeDumper.language, writeToJava)
        typeDumper.write(builder, "\t${typeDumper.fieldName(tableIdField, tableId)}")

        for ((colName, colId) in columns) {
            val colKey = namer.name(colName, colId, typeDumper.language, writeToJava)
            typeDumper.write(builder, "\t${typeDumper.fieldName(colKey, colId)}")
        }

        builder.appendLine()
        builder.append("\t${buildRowClass(rows)}")
        builder.appendLine()

        builder.appendLine("\t}")
        return builder
    }

    private fun buildRowClass(rows: List<GameValElement>): StringBuilder {
        val builder = StringBuilder()
        val classDeclaration = if (typeDumper.language == Language.JAVA) "public static final class" else "object"

        builder.appendLine("\t$classDeclaration ${formatForClassName("Row")} {")
        namer.used.clear()

        rows.forEach {
            val rowKey = namer.name(it.name, it.id.toString(), typeDumper.language, writeToJava)
            typeDumper.write(builder, "\t\t${typeDumper.fieldName(rowKey, it.id.toString())}")
        }

        builder.appendLine("\t\t}")
        return builder
    }

}
