package dev.openrune.cache.worldmap.rasterizer.provider

import dev.openrune.definition.type.SpriteType

/**
 * @author Kris | 21/08/2022
 */
interface SpriteProvider {
    fun getSprites(): Map<Int, SpriteType>?
    val verdana11PtId: Int get() = VERDANA_11_REGULAR_FONT_SPRITE_GROUP
    val verdana13ptId: Int get() = VERDANA_13_REGULAR_FONT_SPRITE_GROUP
    val verdana15ptId: Int get() = VERDANA_15_REGULAR_FONT_SPRITE_GROUP

    private companion object {
        private const val VERDANA_11_REGULAR_FONT_SPRITE_GROUP = 1442
        private const val VERDANA_13_REGULAR_FONT_SPRITE_GROUP = 1445
        private const val VERDANA_15_REGULAR_FONT_SPRITE_GROUP = 1447
    }
}

