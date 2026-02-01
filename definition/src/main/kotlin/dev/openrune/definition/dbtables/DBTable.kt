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
    val rows: List<DBRow>,
    val serverOnly : Boolean = false
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

fun dbTable(tableId: String, serverOnly : Boolean = false,block: DBTableBuilder.() -> Unit): DBTable {
    val rscmId = ConstantProvider.getMapping(tableId) ?: error("Invalid RSCM mapping for tableId: $tableId")
    val rscmName = tableId.substringAfter(".")

    return DBTableBuilder(rscmName,rscmId,serverOnly = serverOnly).apply(block).build()
}

fun dbTable(tableId: Int, serverOnly : Boolean = false,block: DBTableBuilder.() -> Unit): DBTable {
    return DBTableBuilder(tableId,serverOnly = serverOnly).apply(block).build()
}

val tableNames: MutableMap<Int, String> = mutableMapOf()
val columnNames: MutableMap<Int, String> = mutableMapOf()
val rowNames: MutableMap<Int, String> = mutableMapOf()

class DBTableBuilder(private val tableId: Int, private val tableRscmName: String? = null,private var serverOnly : Boolean = false) {

    constructor(name: String, tableId: Int,serverOnly: Boolean = false) : this(tableId, name,serverOnly) {
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
     * Sets whether this task is server-only.
     *
     * @param state `true` to mark the task as server-only, `false` to mark it as not server-only.
     */
    fun serverOnly(state: Boolean) {
        serverOnly = state
    }

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
        return DBTable(tableId, tableRscmName, columns, rows,serverOnly)
    }
}

class DBRowBuilder(
    private val tableColumns: MutableMap<Int, DBColumnType>,
    private val rowId: Int,
    private val rowRscmName: String? = null
) {

    private val columns = mutableMapOf<Int, Array<Any>>()

    fun columnRSCM(id: Int, values: List<String>) {
        putMapped(id, values)
    }

    fun columnRSCM(id: Int, vararg values: String) {
        putMapped(id, values.asList())
    }

    private fun putMapped(id: Int, values: List<String>) {
        columns[id] = values
            .map(ConstantProvider::getMapping)
            .toTypedArray()
    }

    fun column(id: Int, vararg values: Any) {
        putRaw(id, flatten(values))
    }

    fun column(id: Int, values: List<Any>) {
        putRaw(id, values)
    }

    private fun putRaw(id: Int, values: List<Any>) {
        columns[id] = values.toTypedArray()
    }

    private fun flatten(values: Array<out Any>): List<Any> =
        values.flatMap { v ->
            when (v) {
                is IntArray -> v.toList()
                is LongArray -> v.toList()
                is ShortArray -> v.toList()
                is ByteArray -> v.toList()
                is DoubleArray -> v.toList()
                is FloatArray -> v.toList()
                is Array<*> -> {
                    require(v.none { it == null }) {
                        "Column contains null value"
                    }
                    @Suppress("UNCHECKED_CAST")
                    (v as Array<Any>).toList()
                }
                else -> listOf(v)
            }
        }

    fun build(): DBRow =
        DBRow(rowId, rowRscmName, columns)
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