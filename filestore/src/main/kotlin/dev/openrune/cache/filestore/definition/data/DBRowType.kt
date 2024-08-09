package dev.openrune.cache.filestore.definition.data

import dev.openrune.cache.filestore.definition.Definition
import dev.openrune.cache.util.ScriptVarType
import kotlinx.serialization.Contextual
import kotlinx.serialization.Polymorphic

import kotlinx.serialization.Serializable

@Serializable
sealed class DataValue

@Serializable
data class IntValue(val value: Int) : DataValue()

@Serializable
data class StringValue(val value: String) : DataValue()

@Serializable
class DBRowType(override var id: Int = -1, override var inherit: Int = -1): Definition {
    var tableId = 0
    var columnTypes: Array<Array<ScriptVarType>?>? = null
    @Contextual
    var columnValues: Array<Array<@Polymorphic DataValue?>?>? = null
}