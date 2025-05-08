package dev.openrune.definition.type

import dev.openrune.definition.Definition

class DBRowType(
    override var id: Int = -1,
    override var inherit: Int = 1,
    override var debugName : String = "",
): Definition {
    var tableId = 0
    var columns: MutableMap<Int, DBColumnType> = mutableMapOf()
    override fun toString(): String {
        return "DBRowType(id=$id, tableId=$tableId, columns=$columns)"
    }
}