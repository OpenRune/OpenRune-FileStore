package dev.openrune.cache.tools.dbtables

import dev.openrune.definition.RSCMHandler
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

data class FullDBRow(
    val rowId: Int,
    val columns: Map<Int, Array<Any>>
)

fun DBTable.toFullRows(): List<FullDBRow> {
    val result = mutableListOf<FullDBRow>()

    for (row in this.rows) {
        val fullColumns = mutableMapOf<Int, Array<Any>>()

        for ((colId, columnDef) in this.columns) {
            val value = row.columns[colId]
            if (value != null) {
                fullColumns[colId] = value
            } else {
                val default = columnDef.values
                if (default != null) {
                    fullColumns[colId] = default
                } else {
                    // If no value and no default, fill with empty array
                    fullColumns[colId] = emptyArray()
                }
            }
        }

        result.add(FullDBRow(row.rowId, fullColumns))
    }

    return result
}

inline fun <reified T> DBTable.toDatabaseTable(constructor: (FullDBRow) -> T): List<T> {
    return this.toFullRows().map(constructor)
}

data class DBRow(
    val rowId: Int,
    val columns: Map<Int, Array<Any>>
)

fun dbTable(tableId: String, block: DBTableBuilder.() -> Unit): DBTable {
    val rscmId = RSCMHandler.getMapping(tableId) ?: error("Invalid RSCM mapping for tableId: $tableId")
    val rscmName = tableId.substringAfter(".")

    return DBTableBuilder(rscmName,rscmId).apply(block).build()
}

fun dbTable(tableId: Int, block: DBTableBuilder.() -> Unit): DBTable {
    return DBTableBuilder(tableId).apply(block).build()
}

val tableNames: MutableMap<Int, String> = mutableMapOf()
val columnNames: MutableMap<Int, String> = mutableMapOf()
val rowNames: MutableMap<Int, String> = mutableMapOf()

class DBTableBuilder(private val tableId: Int) {

    constructor(name: String, tableId: Int) : this(tableId) {
        tableNames[tableId] = name
    }

    private val columns = mutableMapOf<Int, DBColumnType>()
    private val rows = mutableListOf<DBRow>()

    fun column(name : String,id: Int, types: Array<Type>, values: Array<Any>? = null) {
        if (name.isNotEmpty()) {
            columnNames[id] = name
        }
        columns[id] = DBColumnType(types, values)
    }

    fun column(id: Int, types: Array<Type>, values: Array<Any>? = null) {
        columns[id] = DBColumnType(types, values)
    }

    fun row(rowId: Int, block: DBRowBuilder.() -> Unit) {
        val builder = DBRowBuilder(rowId).apply(block)
        rows.add(builder.build())
    }

    fun row(rowId: String, block: DBRowBuilder.() -> Unit) {
        val rscmId = RSCMHandler.getMapping(rowId) ?: error("Invalid RSCM mapping for rowId: $rowId")
        val rscmName = rowId.substringAfter(".")
        if (rscmName.isNotEmpty()) {
            rowNames[rscmId] = rscmName
        }

        row(rowId = rscmId, block = block)
    }

    fun build(): DBTable {
        return DBTable(tableId, columns, rows)
    }
}

class DBRowBuilder(private val rowId: Int) {
    private val columns = mutableMapOf<Int, Array<Any>>()

    fun columnRSCM(id: Int, values: Array<String>) {
        val resolvedValues: List<Any> = values.map { value ->
            RSCMHandler.getMapping(value)
                ?: error("Invalid RSCM mapping for value: $value")
        }
        columns[id] = resolvedValues.toTypedArray()
    }

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