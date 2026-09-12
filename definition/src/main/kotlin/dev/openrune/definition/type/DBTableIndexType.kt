package dev.openrune.definition.type

import dev.openrune.definition.type.builders.DBTableIndexTypeBuilder

import dev.openrune.definition.Definition
import dev.openrune.definition.util.BaseVarType

sealed class DBTableIndexKey {
    data class IntKey(val value: Int) : DBTableIndexKey()
    data class LongKey(val value: Long) : DBTableIndexKey()
    data class StringKey(val value: String) : DBTableIndexKey()
}

data class DBTableIndexColumn(
    val valueType: BaseVarType,
    val valueToRowIds: Map<DBTableIndexKey, List<Int>>,
)

/**
 * A loaded db-table index. Immutable apart from [id] (which the load machinery assigns):
 * decoding builds one through [DBTableIndexTypeBuilder], and everything after that only reads.
 */
data class DBTableIndexType(
    override var id: Int = -1,
    val columns: List<DBTableIndexColumn> = emptyList(),
) : Definition {

    /** A mutable copy of this definition, for tools that need to edit and re-pack it. */
    fun toBuilder(): DBTableIndexTypeBuilder = DBTableIndexTypeBuilder.from(this)
}
