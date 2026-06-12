package dev.openrune.definition.dbtables

import dev.openrune.definition.type.DBColumnType
import dev.openrune.definition.util.BaseVarType
import dev.openrune.definition.util.CacheVarLiteral
import java.io.File

/** Serializes a [DBTable] (from the Kotlin DSL or elsewhere) to the [DBTableToml] file format. */
fun DBTable.toToml(): String = DBTableToml.write(this)

object DBTableTomlWriter {

    fun write(table: DBTable): String = buildString {
        appendLine("[dbtable]")
        appendLine("id = ${DBTableCellCodec.encodeTableId(table.tableId, table.rscmName)}")
        appendLine("server_only = ${table.serverOnly}")
        if (table.inherit != null) {
            appendLine("inherit = ${quoteTomlString(table.inherit)}")
        }
        appendLine()

        val sortedColumns = table.columns.entries.sortedBy { it.key }
        if (sortedColumns.isNotEmpty()) {
            appendLine("[dbtable.col]")
            for ((_, column) in sortedColumns) {
                val name = column.rscmName ?: error("column missing name")
                appendLine("${name} = ${formatColumnTypes(column.types)}")
            }
            appendLine()
        }

        for (row in table.rows) {
            appendLine("[[row]]")
            appendLine("id = ${DBTableCellCodec.encodeRowId(row.rowId, row.rscmName)}")
            for ((colId, values) in row.columns.entries.sortedBy { it.key }) {
                val column = table.columns[colId] ?: continue
                val name = column.rscmName ?: columnNames[colId] ?: "col_$colId"
                appendLine("$name = ${formatCellValues(values, column.types)}")
            }
            appendLine()
        }
    }.trimEnd() + "\n"

    fun write(table: DBTable, file: File) {
        file.parentFile?.mkdirs()
        file.writeText(write(table))
    }

    private fun formatColumnTypes(types: Array<CacheVarLiteral>): String {
        if (types.size == 1) {
            return quoteTomlString(types[0].name)
        }
        return "[${types.joinToString(", ") { quoteTomlString(it.name) }}]"
    }

    private fun formatCellValues(values: Array<Any>, types: Array<CacheVarLiteral>): String {
        if (values.isEmpty()) return "[]"
        if (values.size == 1) {
            return formatCellValue(values[0], types.firstOrNull() ?: CacheVarLiteral.INT)
        }
        if (shouldUseNestedPairs(types)) {
            return formatNestedPairs(values, types)
        }
        return formatFlatArray(values, types)
    }

    private fun shouldUseNestedPairs(types: Array<CacheVarLiteral>): Boolean {
        if (types.size < 2 || types.size % 2 != 0) return false
        for (i in types.indices step 2) {
            if (!isLabelType(types[i])) return false
            if (!isNumericType(types[i + 1])) return false
        }
        return true
    }

    private fun isLabelType(type: CacheVarLiteral): Boolean =
        type.baseType == BaseVarType.STRING || type.name in labelTypes

    private fun isNumericType(type: CacheVarLiteral): Boolean =
        type.baseType == BaseVarType.INTEGER || type.name == "BOOLEAN"

    private val labelTypes = setOf(
        "STAT", "OBJ", "NPC", "LOC", "ENUM", "SEQ", "AREA", "MAPELEMENT", "STRUCT", "DBROW",
    )

    private fun formatNestedPairs(values: Array<Any>, types: Array<CacheVarLiteral>): String {
        val pairs = values.toList().chunked(2)
        val typePairs = types.toList().chunked(2)
        return buildString {
            append('[')
            pairs.forEachIndexed { index, pair ->
                if (index > 0) append(',')
                appendLine()
                append("    [")
                pair.forEachIndexed { i, value ->
                    if (i > 0) append(", ")
                    val type = typePairs.getOrNull(index)?.getOrNull(i) ?: CacheVarLiteral.INT
                    append(formatCellValue(value, type))
                }
                append(']')
            }
            if (pairs.isNotEmpty()) appendLine()
            append(']')
        }
    }

    private fun formatFlatArray(values: Array<Any>, types: Array<CacheVarLiteral>): String =
        buildString {
            append('[')
            values.forEachIndexed { index, value ->
                if (index > 0) append(", ")
                val type = types.getOrNull(index) ?: types.lastOrNull() ?: CacheVarLiteral.INT
                append(formatCellValue(value, type))
            }
            append(']')
        }

    private fun formatCellValue(value: Any, type: CacheVarLiteral): String = when (value) {
        is Boolean -> value.toString()
        is Int -> when (type.name) {
            "COORDGRID" -> CoordGridCodec.formatExport(value)
            else -> DBTableCellCodec.encodeInt(value, type)
        }
        is Long -> DBTableCellCodec.encodeInt(value.toInt(), type)
        is Short -> DBTableCellCodec.encodeInt(value.toInt(), type)
        is Byte -> DBTableCellCodec.encodeInt(value.toInt(), type)
        is String -> quoteTomlString(value)
        else -> quoteTomlString(value.toString())
    }

    private fun quoteTomlString(value: String): String {
        val escaped = value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\t", "\\t")
            .replace("\n", "\\n")
        return "\"$escaped\""
    }
}
