package dev.openrune.definition.type

import dev.openrune.definition.Definition
import dev.openrune.definition.serialization.Rscm
import kotlinx.serialization.Serializable

@Serializable
data class VarpType(
    override var id: Rscm = -1,
    var configType: Int = 0,

) : Definition