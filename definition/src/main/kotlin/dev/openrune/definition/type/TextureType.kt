package dev.openrune.definition.type

import dev.openrune.definition.Definition
import dev.openrune.definition.game.render.util.JagexColor
import kotlin.math.pow

val DEFAULT_TEXTURE_SIZE = 128

data class TextureType(
    override var id: Int = -1,
    var isTransparent : Boolean = false,
    var fileId : Int = -1,
    var averageRgb : Int = 0,
    var animationDirection : Int = 0,
    var animationSpeed : Int = 0,
    var isLowDetail : Boolean = false
) : Definition {

    private var pixels: IntArray? = null

    fun load(sprites: Map<Int, SpriteType>): IntArray? {
        return pixels ?: run {
            initializePixels(JagexColor.BRIGHTNESS_MAX, DEFAULT_TEXTURE_SIZE, sprites)
            pixels
        }
    }

    private fun initializePixels(brightnessFactor: Double, textureSize: Int, sprites: Map<Int, SpriteType>): Boolean {
        val pixelCount = textureSize * textureSize
        pixels = IntArray(pixelCount)

        val sprite = sprites[fileId]?.sprites?.first() ?: return false
        val raster = sprite.raster
        val palette = sprite.palette

        palette.forEachIndexed { idx, color ->
            palette[idx] = adjustColorBrightness(color, brightnessFactor)
        }

        if (textureSize == sprite.width) {
            var pixelIndex = 0
            while (pixelIndex < pixelCount) {
                pixels!![pixelIndex] = palette[raster[pixelIndex].toInt() and 255]
                pixelIndex++
            }
        } else if (sprite.width == 64 && textureSize == 128) {
            var pixelIndex = 0
            for (y in 0 until textureSize) {
                for (x in 0 until textureSize) {
                    pixels!![pixelIndex++] = palette[raster[(y shr 1 shl 6) + (x shr 1)].toInt() and 255]
                }
            }
        } else if (sprite.height == 128 && textureSize == 64) {
            var pixelIndex = 0
            for (y in 0 until textureSize) {
                for (x in 0 until textureSize) {
                    pixels!![pixelIndex++] = palette[raster[(x shl 1) + (y shl 1 shl 7)].toInt() and 255]
                }
            }
        }
        return true
    }

    fun adjustColorBrightness(color: Int, brightnessFactor: Double): Int {
        val red = ((color shr 16) and 255).toDouble() / 256.0
        val green = ((color shr 8) and 255).toDouble() / 256.0
        val blue = (color and 255).toDouble() / 256.0

        val adjustedRed = (red.pow(brightnessFactor) * 256.0).toInt()
        val adjustedGreen = (green.pow(brightnessFactor) * 256.0).toInt()
        val adjustedBlue = (blue.pow(brightnessFactor) * 256.0).toInt()

        return adjustedBlue + (adjustedGreen shl 8) + (adjustedRed shl 16)
    }


    fun isLowMem(): Boolean {
        return isLowDetail
    }

 }