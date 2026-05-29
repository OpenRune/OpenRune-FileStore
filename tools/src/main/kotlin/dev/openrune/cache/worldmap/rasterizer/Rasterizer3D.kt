package dev.openrune.cache.worldmap.rasterizer

import kotlin.math.pow

/**
 * @author Kris | 15/08/2022
 */
data class Rasterizer3D(
    val brightness: Double = 0.7,
    val colourPalette: IntArray = IntArray(65536)
) {

    init {
        buildPalette(brightness, 0, 512)
    }

    private fun buildPalette(
        brightness: Double,
        @Suppress("SameParameterValue") offset: Int,
        @Suppress("SameParameterValue") until: Int
    ) {
        var index = offset * 128
        for (packedHueAndSaturation in offset until until) {
            val hue = (packedHueAndSaturation shr 3).toDouble() / 64.0 + 0.0078125
            val saturation = (packedHueAndSaturation and 7).toDouble() / 8.0 + 0.0625
            for (lightness in 0..127) {
                val lightnessPercentage = lightness.toDouble() / 128.0
                var redPercentage = lightnessPercentage
                var greenPercentage = lightnessPercentage
                var bluePercentage = lightnessPercentage
                if (saturation != 0.0) {
                    val a: Double = if (lightnessPercentage < 0.5) {
                        lightnessPercentage * (1.0 + saturation)
                    } else {
                        lightnessPercentage + saturation - lightnessPercentage * saturation
                    }
                    val b = 2.0 * lightnessPercentage - a
                    var c = hue + 0.3333333333333333
                    if (c > 1.0) {
                        --c
                    }
                    var var27 = hue - 0.3333333333333333
                    if (var27 < 0.0) {
                        ++var27
                    }
                    redPercentage = if (6.0 * c < 1.0) {
                        b + (a - b) * 6.0 * c
                    } else if (2.0 * c < 1.0) {
                        a
                    } else if (3.0 * c < 2.0) {
                        b + (a - b) * (0.6666666666666666 - c) * 6.0
                    } else {
                        b
                    }
                    greenPercentage = if (6.0 * hue < 1.0) {
                        b + (a - b) * 6.0 * hue
                    } else if (2.0 * hue < 1.0) {
                        a
                    } else if (3.0 * hue < 2.0) {
                        b + (a - b) * (0.6666666666666666 - hue) * 6.0
                    } else {
                        b
                    }
                    bluePercentage = if (6.0 * var27 < 1.0) {
                        b + (a - b) * 6.0 * var27
                    } else if (2.0 * var27 < 1.0) {
                        a
                    } else if (3.0 * var27 < 2.0) {
                        b + (a - b) * (0.6666666666666666 - var27) * 6.0
                    } else {
                        b
                    }
                }
                val red = (redPercentage * 256.0).toInt()
                val green = (greenPercentage * 256.0).toInt()
                val blue = (bluePercentage * 256.0).toInt()
                var rgb = blue + (green shl 8) + (red shl 16)
                rgb = adjustBrightness(rgb, brightness)
                if (rgb == 0) {
                    rgb = 1
                }
                colourPalette[index++] = rgb
            }
        }
    }

    private fun adjustBrightness(rgb: Int, brightness: Double): Int {
        var red = (rgb shr 16).toDouble() / 256.0
        var green = (rgb shr 8 and 255).toDouble() / 256.0
        var blue = (rgb and 255).toDouble() / 256.0
        red = red.pow(brightness)
        green = green.pow(brightness)
        blue = blue.pow(brightness)
        val newRed = (red * 256.0).toInt()
        val newGreen = (green * 256.0).toInt()
        val newBlue = (blue * 256.0).toInt()
        return newBlue + (newGreen shl 8) + (newRed shl 16)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Rasterizer3D

        if (!colourPalette.contentEquals(other.colourPalette)) return false

        return true
    }

    override fun hashCode(): Int {
        return colourPalette.contentHashCode()
    }
}
