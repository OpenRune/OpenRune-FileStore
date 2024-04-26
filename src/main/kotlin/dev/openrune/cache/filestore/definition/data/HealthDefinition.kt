package dev.openrune.cache.filestore.definition.data

import dev.openrune.cache.filestore.definition.Definition
import dev.openrune.cache.filestore.definition.Sound

data class HealthDefinition(
    override var id: Int = -1,
    var int1: Short = 255,
    var int2: Short = 255,
    var int3: Int? = null,
    var int4: Int = 70,
    var frontSpriteId: Int? = null,
    var backSpriteId: Int? = null,
    var width: Short = 30,
    var widthPadding: Short = 0
) : Definition