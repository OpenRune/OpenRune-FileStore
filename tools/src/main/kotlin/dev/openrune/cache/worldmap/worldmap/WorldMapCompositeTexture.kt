package dev.openrune.cache.worldmap.worldmap

import dev.openrune.filesystem.Cache
import java.awt.image.BufferedImage

/**
 * Contains the composite texture image of each world map area.
 * Each pixel on the image corresponds to a single tile; the image only draws underlays and overlays.
 * @author Kris | 14/08/2022
 */
data class WorldMapCompositeTexture(val image: BufferedImage) {
    companion object {
        fun decode(cache: Cache, internalName: String): WorldMapCompositeTexture? {
            return null
        }
    }
}
