package dev.openrune.definition.dbtables

import dev.openrune.definition.constants.ConstantProvider

@Deprecated("Potentially deprecated. Please use the new solution.")
abstract class DatabaseRow(val row: FullDBRow)

interface ColumnType<T> {
    fun deserialize(value: Any): T
}

object IntType : ColumnType<Int> {
    override fun deserialize(value: Any): Int = value as Int
}

object StringType : ColumnType<String> {
    override fun deserialize(value: Any): String = value as String
}

fun <T> DatabaseRow.column(columnName: String, type: ColumnType<T>): T {
    val columnId = columnName.toColumnId()
    val value = row.columns[columnId]?.firstOrNull()
        ?: error("Missing column $columnName")
    return type.deserialize(value)
}

fun <T> DatabaseRow.optionalColumn(columnName: String, type: ColumnType<T>): T? {
    val columnId = columnName.toColumnId()
    val value = row.columns[columnId]?.firstOrNull()
    return value?.let { type.deserialize(it) }
}

fun <A, B> DatabaseRow.optionalColumn(columnName: String, typeA: ColumnType<A>, typeB: ColumnType<B>): Pair<A, B>? {
    val columnId = columnName.toColumnId()
    val value = row.columns[columnId]?.firstOrNull()
    val a = value?.let { typeA.deserialize(it) }
    val b = value?.let { typeB.deserialize(it) }
    if(a == null || b == null)
        return null
    return a to b
}

fun <A, B> DatabaseRow.column(columnName: String, typeA: ColumnType<A>, typeB: ColumnType<B>): Pair<A, B> {
    val columnId = columnName.toColumnId()
    val values = row.columns[columnId] ?: error("Missing column $columnName")
    require(values.size == 2) { "Expected 2 values for tuple2 $columnName" }
    return typeA.deserialize(values[0]) to typeB.deserialize(values[1])
}

fun <A, B, C> DatabaseRow.multiColumn(columnName: String, typeA: ColumnType<A>, typeB: ColumnType<B>, typeC: ColumnType<C>): List<Triple<A, B, C>> {
    val columnId = columnName.toColumnId()
    val values = row.columns[columnId] ?: emptyArray()
    require(values.size % 3 == 0) { "multiColumn expects groups of 3 for $columnName" }
    return values.toList().chunked(3).map { (a, b, c) ->
        Triple(typeA.deserialize(a), typeB.deserialize(b), typeC.deserialize(c))
    }
}

fun <A, B, C> DatabaseRow.multiColumn(columnId: Int, typeA: ColumnType<A>, typeB: ColumnType<B>, typeC: ColumnType<C>): List<Triple<A, B, C>> {
    val values = row.columns[columnId] ?: emptyArray()
    require(values.size % 3 == 0) { "multiColumn expects groups of 3 for $columnId" }
    return values.toList().chunked(3).map { (a, b, c) ->
        Triple(typeA.deserialize(a), typeB.deserialize(b), typeC.deserialize(c))
    }
}

fun String.toColumnId(): Int {
    return ConstantProvider.getMapping(this) ?: this.toIntOrNull()?: error("Invalid key")
}
