package dev.openrune.definition.type

import dev.openrune.definition.Definition
import dev.openrune.definition.util.Type

class DBRowType(override var id: Int = -1, override var inherit: Int = -1): Definition {
    var tableId = 0
    var columnTypes: Array<Array<Type>?>? = null
    var columnValues: Array<Array<Any>?>? = null
}