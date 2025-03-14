package dev.openrune.definition.type

import dev.openrune.definition.Definition
import dev.openrune.definition.serialization.Rscm

data class VarClientType(
    override var id: Int = -1,
    var persist: Boolean = false
) : Definition