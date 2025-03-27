package dev.openrune.definition.type

import dev.openrune.definition.Definition
import dev.openrune.definition.serialization.Rscm
import kotlinx.serialization.Serializable

@Serializable
data class VarBitType(
    override var id: Rscm = -1,
    var varp: Int = 0,
    var startBit: Int = 0,
    var endBit: Int = 0,

) : Definition