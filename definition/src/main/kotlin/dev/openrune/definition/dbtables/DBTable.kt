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

    /**
     * Creates an array containing this [VarType] repeated [count] times.
     *
     * Useful when defining table columns that store multiple values of the same type.
     *
     * Example:
     * ```
     * column("items", ITEMS, VarType.OBJ.count(3))
     * ```
     *
     * @param count The number of times to repeat this type.
     * @return An array of [VarType] with size [count], each entry equal to this type.
     */
    fun VarType.count(count: Int): Array<VarType> = Array(count) { this }



    /**
     * Column with optional name and optional values.
     * Supports single or multiple VarTypes.
     */
    fun column(name: String = "", id: Int, vararg types: VarType) {
        if (name.isNotEmpty()) columnNames[id] = name
        columns[id] = DBColumnType(Array(types.size) { i -> types[i] }, null, name)
    }

    fun column(name: String, id: Int, types: Array<VarType>, values: Array<Any>? = null) {
        if(values != null) {
            require(values.size == types.size) {
                "When providing default values for DBTable '${tableRscmName}', you must supply exactly one default for each column type."
            }
        }
        if (name.isNotEmpty()) columnNames[id] = name
        columns[id] = DBColumnType(types, values, name)
    }

    fun column(id: Int, types: Array<VarType>, values: Array<Any>? = null) {
        if(values != null) {
            require(values.size == types.size) {
                "$tableRscmName has invalid default values: provided ${values.size}, " +
                        "but the table defines ${types.size} column types."
            }
        }
        columns[id] = DBColumnType(types, values)
    }

    /**
     * Row by Int ID
     */
    fun row(rowId: Int, block: DBRowBuilder.() -> Unit) {
        val builder = DBRowBuilder(columns, rowId).apply(block)
        rows.add(builder.build())
    }

    /**
     * Row by RSCM string ID
     */
    fun row(rowId: String, block: DBRowBuilder.() -> Unit) {
        val rscmId = ConstantProvider.getMapping(rowId)
        val rscmName = rowId.substringAfter(".")
        if (rscmName.isNotEmpty()) {
            rowNames[rscmId] = rscmName
        }
        val builder = DBRowBuilder(columns, rscmId, rscmName).apply(block)
        rows.add(builder.build())
    }

    fun build(): DBTable {
        return DBTable(tableId, tableRscmName, columns, rows)
    }
}

class DBRowBuilder(private val tableColumns: MutableMap<Int, DBColumnType>, private val rowId: Int, private val rowRscmName: String? = null) {
    private val columns = mutableMapOf<Int, Array<Any>>()

    fun columnRSCM(id: Int, vararg values: String) {
        columns[id] = values.map { value ->
            ConstantProvider.getMapping(value)
        }.toTypedArray()
        validate(id)
    }

    fun column(id: Int, vararg values: Any) {
        columns[id] = values.toList().toTypedArray()
        validate(id)
    }

    fun column(id: Int, values: List<Any>) {
        columns[id] = values.toTypedArray()
        validate(id)
    }

    private fun validate(id: Int) {
        val values: Array<Any> = columns[id]!!
        val columnType = tableColumns[id]!!
        val types: Array<VarType> = columnType.types

        if (values.size > types.size) {
            val varType = types[0]
            val homozygousTypes = types.isNotEmpty() && types.all { it == varType }

            //if it is mixed vartypes or defaults are set we do not resize.
            require(homozygousTypes && columnType.values == null) {
                "'$rowRscmName' on column $id. Dynamically resizing of varTypes is only allowed with homozygous arrays" +
                        " that do not have set defaults. Expected ${types.size} columns, but found ${values.size}."
            }
            tableColumns[id] = DBColumnType(Array(values.size) { varType }, null, columnType.rscmName)
        }
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