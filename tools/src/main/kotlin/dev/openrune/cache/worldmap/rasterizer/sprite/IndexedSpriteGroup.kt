package dev.openrune.cache.worldmap.rasterizer.sprite

import dev.openrune.cache.worldmap.rasterizer.provider.SpriteProvider
import dev.openrune.definition.game.IndexedSprite

/**
 * @author Kris | 22/08/2022
 */
interface IndexedSpriteGroup {
    fun buildIndexedSprites(group: Int, spriteProvider: SpriteProvider): List<IndexedSprite> {
        val sprite = spriteProvider.getSprites()?.get(group)

        val frames = sprite?.sprites


        return List(frames!!.size) { index ->
            val frame = frames[index]
            frame
        }
    }
}
