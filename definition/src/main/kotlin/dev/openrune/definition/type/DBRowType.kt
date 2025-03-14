package dev.openrune.definition.type

import dev.openrune.definition.Definition

class DBRowType(override var id: Int = -1): Definition {
    var tableId = 0
    var columns: MutableMap<Int, DBColumnType> = mutableMapOf()
}