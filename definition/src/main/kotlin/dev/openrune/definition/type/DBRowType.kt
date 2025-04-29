package dev.openrune.definition.type

import dev.openrune.definition.Definition
import dev.openrune.definition.serialization.Rscm
import kotlinx.serialization.Serializable

@Serializable
class DBRowType(override var id: Rscm = -1): Definition {
    var tableId = 0
    var columns: MutableMap<Int, DBColumnType> = mutableMapOf()
    override fun toString(): String {
        return "DBRowType(id=$id, tableId=$tableId, columns=$columns)"
    }
}