package dev.openrune.definition.type

import dev.openrune.definition.Definition
import dev.openrune.definition.serialization.Rscm

data class InventoryType(
    override var id: Rscm = -1,
    var size: Int = 0
) : Definition