package dev.openrune.definition.type

import dev.openrune.definition.type.builders.TextureTypeBuilder

import dev.openrune.toml.rsconfig.RsTableHeaders
import dev.openrune.definition.Definition
import dev.openrune.definition.game.render.util.JagexColor
import kotlin.math.pow

val DEFAULT_TEXTURE_SIZE = 128

@RsTableHeaders("texture")
data class TextureType(
    override var id: Int = -1,
    val isTransparent : Boolean = false,
    val fileId : Int = -1,
    val averageRgb : Int = 0,
    val animationDirection : Int = 0,
    val animationSpeed : Int = 0,
    val isLowDetail : Boolean = false
) : Definition {

    fun toBuilder(): TextureTypeBuilder = TextureTypeBuilder.from(this)

    private var pixels: IntArray? = null
    private var pixelsBrightness: Double = Double.NaN
    private var pixelsSize: Int = -1

    /** Renders and caches the pixels. [brightness] and [textureSize] are part of the cache key. */
    @JvmOverloads
    fun load(
        sprites: Map<Int, SpriteType>,
        brightness: Double = JagexColor.BRIGHTNESS_LOW,
        textureSize: Int = DEFAULT_TEXTURE_SIZE
    ): IntArray? {
        if (pixels != null && brightness == pixelsBrightness && textureSize == pixelsSize) {
            return pixels
        }
        initializePixels(brightness, textureSize, sprites)
        pixelsBrightness = brightness
        pixelsSize = textureSize
        return pixels
    }

    private fun initializePixels(brightnessFactor: Double, textureSize: Int, sprites: Map<Int, SpriteType>): Boolean {
        val pixelCount = textureSize * textureSize
        pixels = IntArray(pixelCount)

        // Sampled by absolute coordinate below, so a cropped sprite has to be padded out first.
        val sprite = (sprites[fileId]?.sprites?.first() ?: return false).normalized()
        val raster = sprite.raster
        // Into a copy: the palette is shared and gamma is not idempotent.
        val palette = IntArray(sprite.palette.size) { adjustColorBrightness(sprite.palette[it], brightnessFactor) }

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