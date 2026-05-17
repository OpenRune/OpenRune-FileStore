package dev.openrune.definition.type

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

data class DBTableIndexType(
    override var id: Int = -1,
    val columns: MutableList<DBTableIndexColumn> = mutableListOf(),
) : Definition
