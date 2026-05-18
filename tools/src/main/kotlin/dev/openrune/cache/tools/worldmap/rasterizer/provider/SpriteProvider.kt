package dev.openrune.cache.tools.worldmap.rasterizer.provider

/**
 * @author Kris | 21/08/2022
 */
interface SpriteProvider {
    fun getSpriteSheet(id: Int): SpriteSheet?
    val verdana11PtId: Int get() = VERDANA_11_REGULAR_FONT_SPRITE_GROUP
    val verdana13ptId: Int get() = VERDANA_13_REGULAR_FONT_SPRITE_GROUP
    val verdana15ptId: Int get() = VERDANA_15_REGULAR_FONT_SPRITE_GROUP

    private companion object {
        private const val VERDANA_11_REGULAR_FONT_SPRITE_GROUP = 1442
        private const val VERDANA_13_REGULAR_FONT_SPRITE_GROUP = 1445
        private const val VERDANA_15_REGULAR_FONT_SPRITE_GROUP = 1447
    }
}

interface SpriteSheet {
    val width: Int
    val height: Int
    val frames: Array<SpriteFrame>

    fun getHorizontalOffset(alignment: Int): Int {
        val value = when (alignment) {
            0 -> 1
            1 -> 2
            else -> 0
        }
        return when (value) {
            1 -> 0
            2 -> -width / 2
            else -> -width
        }
    }

    fun getVerticalOffset(alignment: Int): Int {
        val value = when (alignment) {
            0 -> 1
            1 -> 2
            else -> 1
        }
        return when (value) {
            1 -> 0
            2 -> -height / 2
            else -> -height
        }
    }
}

interface SpriteFrame {
    val xOffset: Int
    val yOffset: Int
    val innerWidth: Int
    val innerHeight: Int
    val pixels: IntArray
}
