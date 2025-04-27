package dev.openrune.cache.tools.typeDumper.impl

import dev.openrune.cache.CacheManager
import dev.openrune.cache.tools.typeDumper.Language
import dev.openrune.cache.tools.typeDumper.TypeDumper
import dev.openrune.cache.util.Namer
import dev.openrune.cache.util.Namer.Companion.formatForClassName
import dev.openrune.definition.Js5GameValGroup
import dev.openrune.definition.Js5GameValGroup.ROWTYPES

class TablesAndRows(
    private val typeDumper: TypeDumper,
    private val writeToJava: Boolean
) {

    private val namer = Namer()

    fun writeTablesAndRows(group: Js5GameValGroup, data: StringBuilder?, dbRows: Map<String, Int>) {
        val tableColumns = mutableMapOf<String, MutableList<Pair<String, String>>>()
        val tableIds = mutableMapOf<String, Int>()

        if (typeDumper.exportSettings.useGameVal!!.combineTablesAndRows) {
            val (components, pathComponents) = typeDumper.generateWriter(group, override = "DBTables")

            data?.lines()?.forEach { line ->
                val tokens = line.split(".")
                if (tokens.size == 4) {
                    val (table, colName, index, colId) = tokens
                    tableIds[table] = index.toInt()
                    tableColumns.getOrPut(table) { mutableListOf() }.add("COL_$colName" to colId)
                }
            }

            tableColumns.forEach { (table, columns) ->
                val tableId = tableIds[table] ?: -1
                val allRows = CacheManager.getRows().filterValues { it.tableId == tableId }.keys
                val attachedRows = dbRows.filterValues { it in allRows }
                if (typeDumper.language == Language.RSCM) {

                    typeDumper.write(components, "${table}:${tableId}")

                    for ((colName, colId) in columns) {
                        val colKey = namer.name(colName, colId, typeDumper.language, writeToJava)
                        typeDumper.write(components, "${table}.col.${colKey?.replace("COL_","")}:${colId}")
                    }

                    for ((rowName, rowId) in attachedRows) {
                        val rowKey = namer.name(rowName, rowId.toString(), typeDumper.language, writeToJava)
                        typeDumper.write(components, "${table}.row.${rowKey}:${rowId}")
                    }
                } else {
                    typeDumper.write(components, buildClass(table, columns, tableId.toString(), attachedRows).toString())
                }
            }



            typeDumper.endWriter(components, pathComponents)
        } else {
            val dbTables = emptyList<String>().toMutableList()

            data?.lines()?.forEach { line ->
                val tokens = line.split(".")
                if (tokens.size == 4) {
                    val (table, colName, index, colId) = tokens
                    tableIds[table] = index.toInt()
                    val entry = "$table:$index"
                    if (entry !in dbTables) {
                        dbTables.add(entry)
                    }
                }
            }

            typeDumper.writeGeneralGroupData(Js5GameValGroup.TABLETYPES, StringBuilder().apply {
                dbTables.forEach { appendLine(it) }
            }, writeToJava)

            typeDumper.writeGeneralGroupData(ROWTYPES, StringBuilder().apply {
                dbRows.forEach { appendLine("${it.key}:${it.value}") }
            }, writeToJava)

        }

    }

    private fun buildClass(className: String, columns: List<Pair<String, String>>, tableId: String, rows: Map<String, Int>): StringBuilder {
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

    private fun buildRowClass(rows: Map<String, Int>): StringBuilder {
        val builder = StringBuilder()
        val classDeclaration = if (typeDumper.language == Language.JAVA) "public static final class" else "object"

        builder.appendLine("\t$classDeclaration ${formatForClassName("Row")} {")
        namer.used.clear()

        for ((rowName, rowId) in rows) {
            val rowKey = namer.name(rowName, rowId.toString(), typeDumper.language, writeToJava)
            typeDumper.write(builder, "\t\t${typeDumper.fieldName(rowKey, rowId.toString())}")
        }

        builder.appendLine("\t\t}")
        return builder
    }

}
