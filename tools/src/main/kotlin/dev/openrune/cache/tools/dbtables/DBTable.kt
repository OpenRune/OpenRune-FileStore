package dev.openrune.cache.tools.dbtables

import dev.openrune.definition.type.DBColumnType
import dev.openrune.definition.type.DBRowType
import dev.openrune.definition.type.DBTableType
import dev.openrune.definition.util.Type

data class DBTable(
    val tableId: Int,
    val columns: Map<Int, DBColumnType>,
    val rows: List<DBRow>
)

fun DBTable.toDbTableType(): DBTableType {
    val dbTable = DBTableType(this.tableId)
    dbTable.columns.putAll(this.columns)
    return dbTable
}

fun DBTable.toDbRowTypes(): List<DBRowType> {
    val dbRows = mutableListOf<DBRowType>()
    this.rows.forEach { (id, tableColumns) ->
        val row = DBRowType(id)
        row.tableId = this.tableId
        for ((columnId, values) in tableColumns) {
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

data class DBRow(
    val rowId: Int,
    val columns: Map<Int, Array<Any>>
)

fun dbTable(tableId: Int, block: DBTableBuilder.() -> Unit): DBTable {
    return DBTableBuilder(tableId).apply(block).build()
}

class DBTableBuilder(private val tableId: Int) {
    private val columns = mutableMapOf<Int, DBColumnType>()
    private val rows = mutableListOf<DBRow>()

    fun column(id: Int, types: Array<Type>, values: Array<Any>? = null) {
        columns[id] = DBColumnType(types, values)
    }

    fun row(rowId: Int, block: DBRowBuilder.() -> Unit) {
        val builder = DBRowBuilder(rowId).apply(block)
        rows.add(builder.build())
    }

    fun build(): DBTable {
        return DBTable(tableId, columns, rows)
    }
}

class DBRowBuilder(private val rowId: Int) {
    private val columns = mutableMapOf<Int, Array<Any>>()

    fun column(id: Int, values: Array<Any>) {
        columns[id] = values
    }

    fun build(): DBRow {
        return DBRow(rowId, columns)
    }
}

fun generateDsl(table: DBTableType, rows: List<DBRowType>): String {
    val builder = StringBuilder()

    builder.appendLine("val table = dbTable(${table.id}) {")

    for ((columnId, column) in table.columns) {
        val typesString = column.types.joinToString(", ") { "Type.$it" }
        val valuesString = column.values?.joinToString(", ") ?: ""

        if (column.values != null) {
            builder.appendLine("    column($columnId, arrayOf($typesString), arrayOf($valuesString))")
        } else {
            builder.appendLine("    column($columnId, arrayOf($typesString))")
        }
    }

    builder.appendLine()

    for (row in rows) {
        builder.appendLine("    row(${row.id}) {")
        for ((columnId, values) in row.columns) {
            val valuesString = values.values?.joinToString(", ") ?: ""
            builder.appendLine("        column($columnId, arrayOf($valuesString))")
        }
        builder.appendLine("    }")
    }

    builder.appendLine("}")

    return builder.toString()
}