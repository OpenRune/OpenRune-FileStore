package dev.openrune.definition.type

import dev.openrune.definition.Definition

class DBTableType(override var id: Int = -1): Definition {
    var columns: MutableMap<Int, DBColumnType> = mutableMapOf()
}