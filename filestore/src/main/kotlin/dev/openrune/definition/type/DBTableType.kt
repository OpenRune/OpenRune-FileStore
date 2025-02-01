package dev.openrune.definition.type

import dev.openrune.definition.Definition
import dev.openrune.definition.util.ScriptVarType

class DBTableType(override var id: Int = -1, override var inherit: Int = -1): Definition {
    var types: Array<Array<ScriptVarType>?>? = null
    var defaultColumnValues: Array<Array<Any?>?>? = null
}