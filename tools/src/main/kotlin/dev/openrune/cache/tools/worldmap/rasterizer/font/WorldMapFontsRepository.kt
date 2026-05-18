package dev.openrune.cache.tools.worldmap.rasterizer.font

import dev.openrune.cache.tools.worldmap.rasterizer.provider.FontMetrics
import dev.openrune.cache.tools.worldmap.rasterizer.provider.FontMetricsProvider
import dev.openrune.cache.tools.worldmap.rasterizer.provider.SpriteProvider
import dev.openrune.cache.tools.worldmap.rasterizer.provider.SpriteSheet
import dev.openrune.cache.tools.worldmap.rasterizer.utils.WorldMapLabelSize
import dev.openrune.cache.tools.worldmap.rasterizer.sprite.ModIconsSprites
import dev.openrune.cache.tools.worldmap.rasterizer.Font

/**
 * @author Kris | 21/08/2022
 */
data class WorldMapFontsRepository(private val fonts: Map<WorldMapLabelSize, Font>) : Map<WorldMapLabelSize, Font> by fonts {

    companion object {
        fun buildWorldMapFonts(
            spriteProvider: SpriteProvider,
            fontMetricsProvider: FontMetricsProvider,
            modIconsSprites: ModIconsSprites,
        ): WorldMapFontsRepository {
            val verdana11Sprite = spriteProvider.getSpriteSheet(spriteProvider.verdana11PtId) ?: error("Cannot find verdana 11 font sprite.")
            val verdana13Sprite = spriteProvider.getSpriteSheet(spriteProvider.verdana13ptId) ?: error("Cannot find verdana 13 font sprite.")
            val verdana15Sprite = spriteProvider.getSpriteSheet(spriteProvider.verdana15ptId) ?: error("Cannot find verdana 15 font sprite.")
            val verdana11Font = fontMetricsProvider.getFont(fontMetricsProvider.verdana11FontId) ?: error("Cannot find verdana 11 font.")
            val verdana13Font = fontMetricsProvider.getFont(fontMetricsProvider.verdana13FontId) ?: error("Cannot find verdana 13 font.")
            val verdana15Font = fontMetricsProvider.getFont(fontMetricsProvider.verdana15FontId) ?: error("Cannot find verdana 15 font.")
            return WorldMapFontsRepository(
                mapOf(
                    WorldMapLabelSize.Small to createFont(verdana11Sprite, verdana11Font, modIconsSprites),
                    WorldMapLabelSize.Medium to createFont(verdana13Sprite, verdana13Font, modIconsSprites),
                    WorldMapLabelSize.Large to createFont(verdana15Sprite, verdana15Font, modIconsSprites)
                )
            )
        }

        private fun createFont(spriteSheet: SpriteSheet, fontMetrics: FontMetrics, modIconsSprites: ModIconsSprites): Font {
            val xOffsets = IntArray(spriteSheet.frames.size)
            val yOffsets = IntArray(spriteSheet.frames.size)
            val widths = IntArray(spriteSheet.frames.size)
            val heights = IntArray(spriteSheet.frames.size)
            val pixels = Array(spriteSheet.frames.size) { index ->
                val frame = spriteSheet.frames[index]
                xOffsets[index] = frame.xOffset
                yOffsets[index] = frame.yOffset
                widths[index] = frame.innerWidth
                heights[index] = frame.innerHeight
                frame.pixels
            }
            return Font(fontMetrics, xOffsets, yOffsets, widths, heights, pixels, modIconsSprites)
        }
    }
}
