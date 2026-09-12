package dev.openrune.cache.worldmap.worldmap.utils

import kotlin.math.roundToInt

/**
 * @author Kris | 21/08/2022
 */
object WorldMapExt {
    @Strictfp
    fun rgbToHsl(red: Int, green: Int, blue: Int): Int {
        val max = maxOf(red, green, blue).toDouble()
        val min = minOf(red, green, blue).toDouble()
        val range = max - min
        var hue = (max + min) / 2.0
        val lightness = hue
        val saturation = if (max == min) 0.0 else if (lightness > 0.5) (range / (2 - max - min)) else (range / (max + min))
        hue = when (max) {
            min -> 0.0
            red.toDouble() -> (green - blue).toDouble() / range + (if (green < blue) 6 else 0)
            green.toDouble() -> (blue - red).toDouble() / range + 2
            blue.toDouble() -> (red - green).toDouble() / range + 4
            else -> throw IllegalStateException()
        }
        hue /= 6.0
        val convertedHue = (hue * 64.0).roundToInt() and 0x3F
        val convertedSaturation = ((saturation * 8.0).roundToInt() and 0x7)
        val convertedLightness = (lightness * 128.0).roundToInt()
        return (convertedHue shl 10) or ((convertedSaturation and 0x7) shl 7) or (convertedLightness and 0x7F)
    }
}
