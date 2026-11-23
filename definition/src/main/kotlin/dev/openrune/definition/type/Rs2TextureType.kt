package dev.openrune.definition.type

import dev.openrune.definition.Definition

data class Rs2TextureType(
    override var id: Int = -1,
    var averageColour: Int = 0,
    var opaque: Boolean = false,
    var sprite: Int = -1,
    var colourAdjustment: Int = 0,
    var animationDirection: Int = 0,
    var animationSpeed: Int = 0,
) : Definition
