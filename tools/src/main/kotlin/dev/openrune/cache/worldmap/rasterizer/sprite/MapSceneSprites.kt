package dev.openrune.cache.worldmap.rasterizer.sprite

import dev.openrune.cache.worldmap.rasterizer.sprite.IndexedSpriteGroup
import dev.openrune.cache.worldmap.rasterizer.provider.GraphicsDefaultsProvider
import dev.openrune.cache.worldmap.rasterizer.provider.SpriteProvider
import dev.openrune.definition.game.IndexedSprite

/**
 * @author Kris | 22/08/2022
 */
data class MapSceneSprites(val indexedSprites: List<IndexedSprite>) {
    companion object : IndexedSpriteGroup {
        fun build(graphicsDefaultsProvider: GraphicsDefaultsProvider, spriteProvider: SpriteProvider): MapSceneSprites {
            return MapSceneSprites(buildIndexedSprites(graphicsDefaultsProvider.getMapScenesGroup(), spriteProvider))
        }
    }
}
