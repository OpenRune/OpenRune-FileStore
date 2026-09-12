package dev.openrune.definition.type.builders

import dev.openrune.definition.type.*

class DBTableIndexTypeBuilder(var id: Int = -1) {

    var columns: MutableList<DBTableIndexColumn> = mutableListOf()

    fun build(): DBTableIndexType = DBTableIndexType(
        id = id,
        columns = columns.toList(),
    )

    companion object {
        /** Copies every field of [type] into a fresh builder. List contents are copied too. */
        fun from(type: DBTableIndexType): DBTableIndexTypeBuilder {
            val builder = DBTableIndexTypeBuilder(type.id)
            builder.columns = type.columns.toMutableList()
            return builder
        }
    }
}
