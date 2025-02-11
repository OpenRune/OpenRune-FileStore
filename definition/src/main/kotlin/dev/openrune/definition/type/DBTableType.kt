package dev.openrune.definition.type

import dev.openrune.definition.Definition
import dev.openrune.definition.util.Type

class DBTableType(override var id: Int = -1, override var inherit: Int = -1): Definition {
    var columns: Array<DBColumnType?> = emptyArray()

    fun initialize(size: Int) {
        columns = arrayOfNulls(size)
    }
}