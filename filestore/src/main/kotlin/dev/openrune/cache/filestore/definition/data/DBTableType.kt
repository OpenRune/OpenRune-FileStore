package dev.openrune.cache.filestore.definition.data

import dev.openrune.cache.filestore.definition.Definition
import dev.openrune.cache.util.ScriptVarType
import kotlinx.serialization.Contextual
import kotlinx.serialization.Polymorphic

import kotlinx.serialization.Serializable

@Serializable
class DBTableType(override var id: Int = -1, override var inherit: Int = -1): Definition {
    var types: Array<Array<ScriptVarType>?>? = null
    @Contextual
    var defaultColumnValues: Array<Array<@Polymorphic DataValue?>?>? = null
}