package dev.openrune.cache.worldmap.worldmap.rasterizer.utils

import dev.openrune.cache.worldmap.rasterizer.Font

/**
 * @author Kris | 22/08/2022
 */
data class WorldMapTextLabel(
    val name: String,
    val width: Int,
    val height: Int,
    val size: WorldMapLabelSize,
    val font: Font,
)
