package dev.openrune.definition.type

import dev.openrune.definition.Definition

data class MapSceneIconType(
    override var id: Int = -1,
    var graphic: Int? = null,
    var tint: Int = 0,
    var resize: Boolean = false,
    var blankGraphic: Boolean = false,
    var hideOnMinimap: Boolean = false
) : Definition
