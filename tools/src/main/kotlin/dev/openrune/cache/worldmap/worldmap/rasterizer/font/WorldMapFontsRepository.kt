package dev.openrune.cache.worldmap.worldmap.rasterizer.font

import dev.openrune.cache.worldmap.rasterizer.Font
import dev.openrune.cache.worldmap.rasterizer.provider.FontMetrics
import dev.openrune.cache.worldmap.rasterizer.provider.FontMetricsProvider
import dev.openrune.cache.worldmap.rasterizer.provider.SpriteProvider
import dev.openrune.cache.worldmap.rasterizer.sprite.ModIconsSprites
import dev.openrune.cache.worldmap.worldmap.rasterizer.utils.WorldMapLabelSize
import dev.openrune.definition.type.SpriteType

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
            val verdana11Sprite = spriteProvider.getSprites()?.get(spriteProvider.verdana11PtId) ?: error("Cannot find verdana 11 font sprite.")
            val verdana13Sprite = spriteProvider.getSprites()?.get(spriteProvider.verdana13ptId) ?: error("Cannot find verdana 13 font sprite.")
            val verdana15Sprite = spriteProvider.getSprites()?.get(spriteProvider.verdana15ptId) ?: error("Cannot find verdana 15 font sprite.")
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

        private fun createFont(spriteSheet: SpriteType, fontMetrics: FontMetrics, modIconsSprites: ModIconsSprites): Font {
            val glyphCount = 256
            val xOffsets = IntArray(glyphCount)
            val yOffsets = IntArray(glyphCount)
            val widths = IntArray(glyphCount)
            val heights = IntArray(glyphCount)
            val pixels = Array(glyphCount) { IntArray(0) }

            for (index in spriteSheet.sprites.indices) {
                if (index >= glyphCount) break
                val frame = spriteSheet.sprites[index]
                xOffsets[index] = frame.offsetX
                yOffsets[index] = frame.offsetY
                widths[index] = frame.width
                heights[index] = frame.height
                val area = frame.width * frame.height
                if (area > 0 && frame.raster.size == area) {
                    pixels[index] = IntArray(area) { i ->
                        if (frame.raster[i].toInt() and 0xFF != 0) 1 else 0
                    }
                }
            }
            return Font(fontMetrics, xOffsets, yOffsets, widths, heights, pixels, modIconsSprites)
        }
    }
}
