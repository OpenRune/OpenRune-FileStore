package dev.openrune.definition.dbtables

import dev.openrune.definition.constants.ConstantProvider
import dev.openrune.definition.type.DBColumnType
import dev.openrune.definition.type.DBRowType
import dev.openrune.definition.type.DBTableType
import dev.openrune.definition.util.VarType

data class DBTable(
    val tableId: Int,
    val rscmName: String? = null,
    val columns: Map<Int, DBColumnType>,
    val rows: List<DBRow>
)

fun DBTable.toDbTableType(): DBTableType {
    val dbTable = DBTableType(this.tableId)
    dbTable.columns.putAll(this.columns)
    return dbTable
}

@Deprecated("Use extension function: toDbTableType")
fun convertToDBTableType(dbTableWithRows: DBTable): DBTableType {
    return DBTableType(dbTableWithRows.tableId).apply {
        columns.putAll(dbTableWithRows.columns)
    }
}

fun DBTable.toDbRowTypes(): List<DBRowType> {
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

@Deprecated("Use extension function: toDbRowTypes")
fun convertToDBRowType(dbTableWithRows: DBTable): List<DBRowType> {
    return dbTableWithRows.rows.map { row ->
        DBRowType(row.rowId, row.rscmName).apply {
            tableId = dbTableWithRows.tableId
            columns.putAll(row.columns.mapValues { (columnId, values) ->
                DBColumnType(dbTableWithRows.columns[columnId]?.types ?: emptyArray(), values)
            })
        }
    }
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

inline fun <T> toDatabaseTable(table: DBTableType, rows: List<DBRowType>, constructor: (FullDBRow) -> T): List<T> {
    val result = mutableListOf<T>()
    for (row in rows) {
        val fullColumns = mutableMapOf<Int, Array<Any>>()

        for ((colId, columnDef) in table.columns) {
            val value = row.columns[colId]
            if (value?.values != null) {
                fullColumns[colId] = value.values!!
            } else {
                val default = columnDef.values
                if (default != null) {
                    fullColumns[colId] = default
                } else {
                    fullColumns[colId] = emptyArray<Any>()
                }
            }
        }

        result.add(constructor(FullDBRow(row.id, fullColumns)))
    }
    return result
}
data class DBRow(
    val rowId: Int,
    val rscmName: String? = null,
    val columns: Map<Int, Array<Any>>
) {
    override fun toString(): String {
        val cols = columns.entries.joinToString(
            separator = ", ",
            prefix = "{",
            postfix = "}"
        ) { (colId, values) ->
            "$colId=${values.joinToString(prefix = "[", postfix = "]")}"
        }
        return "DBRow(rowId=$rowId, columns=$cols)"
    }
}

fun dbTable(tableId: String, block: DBTableBuilder.() -> Unit): DBTable {
    val rscmId = ConstantProvider.getMapping(tableId) ?: error("Invalid RSCM mapping for tableId: $tableId")
    val rscmName = tableId.substringAfter(".")

    return DBTableBuilder(rscmName,rscmId).apply(block).build()
}

fun dbTable(tableId: Int, block: DBTableBuilder.() -> Unit): DBTable {
    return DBTableBuilder(tableId).apply(block).build()
}

val tableNames: MutableMap<Int, String> = mutableMapOf()
val columnNames: MutableMap<Int, String> = mutableMapOf()
val rowNames: MutableMap<Int, String> = mutableMapOf()

class DBTableBuilder(private val tableId: Int, private val tableRscmName: String? = null) {

    constructor(name: String, tableId: Int) : this(tableId, name) {
        tableNames[tableId] = name
    }

    private val columns = mutableMapOf<Int, DBColumnType>()
    private val rows = mutableListOf<DBRow>()

    fun column(name : String, id: Int, types: Array<VarType>, values: Array<Any>? = null) {
        if (name.isNotEmpty()) {
            columnNames[id] = name
        }
        columns[id] = DBColumnType(types, values, name)
    }

    fun column(id: Int, types: Array<VarType>, values: Array<Any>? = null) {
        columns[id] = DBColumnType(types, values)
    }

    fun row(rowId: Int, block: DBRowBuilder.() -> Unit) {
        val builder = DBRowBuilder(rowId).apply(block)
        rows.add(builder.build())
    }

    fun row(rowId: String, block: DBRowBuilder.() -> Unit) {
        val rscmId = ConstantProvider.getMapping(rowId) ?: error("Invalid RSCM mapping for rowId: $rowId")
        val rscmName = rowId.substringAfter(".")
        if (rscmName.isNotEmpty()) {
            rowNames[rscmId] = rscmName
        }

        val builder = DBRowBuilder(rscmId, rscmName).apply(block)
        rows.add(builder.build())
    }

    fun build(): DBTable {
        return DBTable(tableId, tableRscmName, columns, rows)
    }
}

class DBRowBuilder(private val rowId: Int, private val rowRscmName: String? = null) {
    private val columns = mutableMapOf<Int, Array<Any>>()

    fun columnRSCM(id: Int, values: Array<String>) {
        val resolvedValues: List<Any> = values.map { value ->
            ConstantProvider.getMapping(value)
                ?: error("Invalid RSCM mapping for value: $value")
        }
        columns[id] = resolvedValues.toTypedArray()
    }

    fun column(id: Int, values: Array<Any>) {
        columns[id] = values
    }

    fun build(): DBRow {
        return DBRow(rowId, rowRscmName, columns)
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