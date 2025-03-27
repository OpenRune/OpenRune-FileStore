package dev.openrune.definition.game.render.util

import kotlin.math.abs
import kotlin.math.pow

object JagexColor {
    const val BRIGHTNESS_MAX = 0.6

    private const val HUE_OFFSET = 0.5 / 64.0
    private const val SATURATION_OFFSET = 0.5 / 8.0

    fun unpackHue(hsl: Short) = (hsl.toInt() shr 10) and 63
    fun unpackSaturation(hsl: Short) = (hsl.toInt() shr 7) and 7
    fun unpackLuminance(hsl: Short) = hsl.toInt() and 127

    fun unpackHueFull(hsl: Int) = (hsl shr 16) and 0xFF
    fun unpackSaturationFull(hsl: Int) = (hsl shr 8) and 0xFF
    fun unpackLuminanceFull(hsl: Int) = hsl and 0xFF

    fun HSLtoRGB(hsl: Short, brightness: Double): Int {
        val hue = unpackHue(hsl) / 64.0 + HUE_OFFSET
        val saturation = unpackSaturation(hsl) / 8.0 + SATURATION_OFFSET
        val luminance = unpackLuminance(hsl) / 128.0

        return convertHSLtoRGB(hue, saturation, luminance, brightness)
    }

    fun HSLtoRGBFull(hsl: Int): Int {
        val hue = unpackHueFull(hsl) / 256.0
        val saturation = unpackSaturationFull(hsl) / 256.0
        val luminance = unpackLuminanceFull(hsl) / 256.0

        return convertHSLtoRGB(hue, saturation, luminance, 1.0)
    }

    private fun convertHSLtoRGB(hue: Double, saturation: Double, luminance: Double, brightness: Double): Int {
        val chroma = (1.0 - abs(2.0 * luminance - 1.0)) * saturation
        val x = chroma * (1 - abs((hue * 6.0) % 2.0 - 1.0))
        val lightness = luminance - chroma / 2

        val (r, g, b) = when ((hue * 6).toInt()) {
            0 -> Triple(chroma, x, 0.0)
            1 -> Triple(x, chroma, 0.0)
            2 -> Triple(0.0, chroma, x)
            3 -> Triple(0.0, x, chroma)
            4 -> Triple(x, 0.0, chroma)
            else -> Triple(chroma, 0.0, x)
        }

        val rgb = adjustForBrightness(
            (((r + lightness) * 256).toInt() shl 16) or
                    (((g + lightness) * 256).toInt() shl 8) or
                    ((b + lightness) * 256).toInt(), brightness
        )

        return if (rgb == 0) 1 else rgb
    }

    private fun adjustForBrightness(rgb: Int, brightness: Double): Int {
        val r = ((rgb shr 16) and 255) / 256.0
        val g = ((rgb shr 8) and 255) / 256.0
        val b = (rgb and 255) / 256.0

        return (((r.pow(brightness) * 256).toInt() shl 16) or
                ((g.pow(brightness) * 256).toInt() shl 8) or
                (b.pow(brightness) * 256).toInt())
    }

    @JvmStatic
    fun createPalette(brightness: Double): IntArray {
        return IntArray(65536) { HSLtoRGB(it.toShort(), brightness) }
    }

    fun getRGBFull(hsl: Int) = HSLtoRGBFull(hsl)
}
