package dev.openrune.definition.type

import dev.openrune.definition.Definition
import kotlinx.serialization.Serializable

@Serializable
class DBTableType(override var id: Int = -1): Definition {
    var columns: MutableMap<Int, DBColumnType> = mutableMapOf()
}