package dev.openrune.definition.type

import dev.openrune.definition.Definition
import dev.openrune.definition.serialization.Rscm
import kotlinx.serialization.Serializable

@Serializable
class DBTableType(override var id: Rscm = -1): Definition {
    var columns: MutableMap<Int, DBColumnType> = mutableMapOf()
}