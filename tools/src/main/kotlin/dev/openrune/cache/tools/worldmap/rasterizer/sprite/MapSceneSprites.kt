package dev.openrune.cache.tools.worldmap.rasterizer.sprite

import dev.openrune.cache.tools.worldmap.rasterizer.provider.GraphicsDefaultsProvider
import dev.openrune.cache.tools.worldmap.rasterizer.provider.SpriteProvider
import dev.openrune.cache.tools.worldmap.rasterizer.sprite.IndexedSpriteGroup
import dev.openrune.cache.tools.worldmap.rasterizer.sprite.SingleFrameSprite

/**
 * @author Kris | 22/08/2022
 */
data class MapSceneSprites(val indexedSprites: List<SingleFrameSprite>) {
    companion object : IndexedSpriteGroup {
        fun build(graphicsDefaultsProvider: GraphicsDefaultsProvider, spriteProvider: SpriteProvider): MapSceneSprites {
            return MapSceneSprites(buildIndexedSprites(graphicsDefaultsProvider.getMapScenesGroup(), spriteProvider))
        }
    }
}
