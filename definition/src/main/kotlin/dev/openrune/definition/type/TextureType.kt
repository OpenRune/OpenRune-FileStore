package dev.openrune.definition.type

import dev.openrune.definition.Definition
import dev.openrune.definition.game.render.util.JagexColor
import kotlin.math.pow

val DEFAULT_TEXTURE_SIZE = 128

data class TextureType(
    override var id: Int = -1,
    var isTransparent : Boolean = false,
    var fileIds : MutableList<Int> = emptyList<Int>().toMutableList(),
    var combineModes : MutableList<Int> = emptyList<Int>().toMutableList(),
    var field2440 : MutableList<Int> = emptyList<Int>().toMutableList(),
    var colourAdjustments : MutableList<Int> = emptyList<Int>().toMutableList(),
    var averageRgb : Int = 0,
    var animationDirection : Int = 0,
    var animationSpeed : Int = 0
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

        for (index in fileIds.indices) {
            val sprite = sprites[fileIds[index]]?.sprites?.first() ?: continue
            val raster = sprite.raster
            val palette = sprite.palette
            val colorAdjustment = this.colourAdjustments[index]

            if ((colorAdjustment and -16777216) == 50331648) {
                var red = colorAdjustment and 16711935
                var green = colorAdjustment shr 8 and 255

                var index = 0
                while (index < palette.size) {
                    var color = palette[index]
                    if (color shr 8 == (color and 65535)) {
                        color = color and 255
                        palette[index] = red * color shr 8 and 16711935 or (green * color and 65280)
                    }
                    index++
                }
            }

            palette.forEachIndexed { idx, color ->
                palette[idx] = adjustColorBrightness(color, brightnessFactor)
            }

            val combineMode = if (index == 0) 0 else combineModes[index - 1]

            if (combineMode == 0) {
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
                } else {
                    throw RuntimeException("Unexpected texture size configuration")
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
        return DEFAULT_TEXTURE_SIZE == 64
    }

 }