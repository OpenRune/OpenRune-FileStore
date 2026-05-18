package dev.openrune.cache.tools.worldmap.rasterizer.sprite

import dev.openrune.cache.tools.worldmap.rasterizer.provider.SpriteProvider

/**
 * @author Kris | 22/08/2022
 */
interface IndexedSpriteGroup {
    fun buildIndexedSprites(group: Int, spriteProvider: SpriteProvider): List<SingleFrameSprite> {
        val sprite = spriteProvider.getSpriteSheet(group) ?: return emptyList()
        val frames = sprite.frames
        return List(frames.size) { index ->
            val frame = frames[index]
            val width = sprite.width
            val height = sprite.height
            val xOffset = frame.xOffset
            val yOffset = frame.yOffset
            val subWidth = frame.innerWidth
            val subHeight = frame.innerHeight
            SingleFrameSprite(width, height, xOffset, yOffset, subWidth, subHeight, frame.pixels)
        }
    }
}
