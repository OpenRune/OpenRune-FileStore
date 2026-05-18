package dev.openrune.cache.tools.worldmap

import dev.openrune.cache.tools.worldmap.toImage
import java.awt.image.BufferedImage
import org.openrs2.cache.Cache

/**
 * Contains the composite texture image of each world map area.
 * Each pixel on the image corresponds to a single tile; the image only draws underlays and overlays.
 * @author Kris | 14/08/2022
 */
data class WorldMapCompositeTexture(val image: BufferedImage) {
    companion object {
        fun decode(cache: Cache, internalName: String): WorldMapCompositeTexture {
            require(cache.exists(WORLD_MAP_DATA_ARCHIVE, "compositetexture", internalName))
            val buffer = cache.read(WORLD_MAP_DATA_ARCHIVE, "compositetexture", internalName)
            return WorldMapCompositeTexture(buffer.toImage())
        }
    }
}
