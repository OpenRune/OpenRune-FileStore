package dev.openrune.cache.tools.worldmap.rasterizer.sprite

import dev.openrune.cache.tools.worldmap.rasterizer.provider.GraphicsDefaultsProvider
import dev.openrune.cache.tools.worldmap.rasterizer.provider.SpriteProvider

/**
 * @author Kris | 22/08/2022
 */
class ModIconsSprites(val indexedSprites: List<SingleFrameSprite>) {
    companion object : IndexedSpriteGroup {
        fun build(graphicsDefaultsProvider: GraphicsDefaultsProvider, spriteProvider: SpriteProvider): ModIconsSprites {
            return ModIconsSprites(buildIndexedSprites(graphicsDefaultsProvider.getModIconsGroup(), spriteProvider))
        }
    }
}
