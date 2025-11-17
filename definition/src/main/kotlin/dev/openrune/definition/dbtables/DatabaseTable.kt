package dev.openrune.definition.dbtables

import dev.openrune.definition.type.DBRowType
import dev.openrune.definition.type.DBTableType
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.iterator

@Deprecated("Potentially deprecated. Please use the new solution.")
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