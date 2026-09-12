package dev.openrune.cache.worldmap.worldmap.config

import dev.openrune.cache.worldmap.worldmap.WorldMapFormat

/**
 * @author Kris | 21/08/2022
 */
class WorldMapConfig {
    /** Client/cache revision; world map IO branches at [dev.openrune.cache.worldmap.worldmap.WorldMapFormat.REVISION]. */
    var cacheRevision: Int = WorldMapFormat.REVISION
    var imageType: String = "png"
    var blendBordersSeparately: Boolean = false
    var updateCompositeTexture: Boolean = true
    var updateUnderlayImages: Boolean = true
    var brightness: Double = 0.7

    /** When set, each packed area's composite texture is also written here as an image for inspection. */
    var compositeDumpDirectory: java.nio.file.Path? = null
}
