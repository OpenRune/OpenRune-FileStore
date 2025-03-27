package dev.openrune.definition.type

import dev.openrune.definition.Definition
import dev.openrune.definition.serialization.Rscm
import kotlinx.serialization.Serializable

@Serializable
data class HealthBarType(
    override var id: Rscm = -1,
    var int1: Int = 255,
    var int2: Int = 255,
    var int3: Int = -1,
    var int4: Int = 70,
    var frontSpriteId: Int = -1,
    var backSpriteId: Int = -1,
    var width: Int = 30,
    var widthPadding: Int = 0,

) : Definition