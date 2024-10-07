package dev.openrune.game

import kotlin.math.abs
import kotlin.math.pow

object JagexColor {
    const val BRIGHTNESS_MAX: Double = .6
    const val BRIGHTNESS_HIGH: Double = .7
    const val BRIGHTNESS_LOW: Double = .8
    const val BRIGHTNESS_MIN: Double = .9

    private const val HUE_OFFSET = (.5 / 64.0)
    private const val SATURATION_OFFSET = (.5 / 8.0)

    fun packHSL(hue: Int, saturation: Int, luminance: Int): Short {
        return ((hue and 63).toShort().toInt() shl 10 or ((saturation and 7).toShort()
            .toInt() shl 7
                ) or (luminance and 127).toShort().toInt()).toShort()
    }

    fun packHSLFull(hue: Int, saturation: Int, luminance: Int): Int {
        return (hue and 0xFF) shl 16 or ((saturation and 0xFF) shl 8
                ) or (luminance and 0xFF)
    }

    fun unpackHue(hsl: Short): Int {
        return hsl.toInt() shr 10 and 63
    }

    fun unpackSaturation(hsl: Short): Int {
        return hsl.toInt() shr 7 and 7
    }

    fun unpackLuminance(hsl: Short): Int {
        return hsl.toInt() and 127
    }

    fun unpackHueFull(hsl: Int): Int {
        return (hsl shr 16 and 0xFF)
    }

    fun unpackSaturationFull(hsl: Int): Int {
        return (hsl shr 8 and 0xFF)
    }

    fun unpackLuminanceFull(hsl: Int): Int {
        return (hsl and 0xFF)
    }

    fun formatHSL(hsl: Short): String {
        return String.format("%02Xh%Xs%02Xl", unpackHue(hsl), unpackSaturation(hsl), unpackLuminance(hsl))
    }

    fun HSLtoRGB(hsl: Short, brightness: Double): Int {
        val hue = unpackHue(hsl).toDouble() / 64.0 + HUE_OFFSET
        val saturation = unpackSaturation(hsl).toDouble() / 8.0 + SATURATION_OFFSET
        val luminance = unpackLuminance(hsl).toDouble() / 128.0

        // This is just a standard hsl to rgb transform
        // the only difference is the offsets above and the brightness transform below
        val chroma = (1.0 - abs((2.0 * luminance) - 1.0)) * saturation
        val x = chroma * (1 - abs(((hue * 6.0) % 2.0) - 1.0))
        val lightness = luminance - (chroma / 2)

        var r = lightness
        var g = lightness
        var b = lightness
        when ((hue * 6.0).toInt()) {
            0 -> {
                r += chroma
                g += x
            }

            1 -> {
                g += chroma
                r += x
            }

            2 -> {
                g += chroma
                b += x
            }

            3 -> {
                b += chroma
                g += x
            }

            4 -> {
                b += chroma
                r += x
            }

            else -> {
                r += chroma
                b += x
            }
        }
        var rgb = (((r * 256.0).toInt() shl 16)
                or ((g * 256.0).toInt() shl 8)
                or ((b * 256.0).toInt()))

        rgb = adjustForBrightness(rgb, brightness)

        if (rgb == 0) {
            rgb = 1
        }
        return rgb
    }

    fun HSLtoRGBFull(hsl: Int): Int {
        val hue = unpackHueFull(hsl).toDouble() / 256.0
        val saturation = unpackSaturationFull(hsl).toDouble() / 256.0
        val luminance = unpackLuminanceFull(hsl).toDouble() / 256.0

        // This is just a standard hsl to rgb transform
        // the only difference is the offsets above and the brightness transform below
        val chroma = (1.0 - abs((2.0 * luminance) - 1.0)) * saturation
        val x = chroma * (1 - abs(((hue * 6.0) % 2.0) - 1.0))
        val lightness = luminance - (chroma / 2)

        var r = lightness
        var g = lightness
        var b = lightness
        when ((hue * 6.0).toInt()) {
            0 -> {
                r += chroma
                g += x
            }

            1 -> {
                g += chroma
                r += x
            }

            2 -> {
                g += chroma
                b += x
            }

            3 -> {
                b += chroma
                g += x
            }

            4 -> {
                b += chroma
                r += x
            }

            else -> {
                r += chroma
                b += x
            }
        }
        var rgb = (((((r * 256.0).toInt()) and 255) shl 16)
                or ((((g * 256.0).toInt()) and 255) shl 8)
                or (((b * 256.0).toInt()) and 255))

        if (rgb == 0) {
            rgb = 1
        }
        return rgb
    }

    fun adjustForBrightness(rgb: Int, brightness: Double): Int {
        var r = (rgb shr 16).toDouble() / 256.0
        var g = (rgb shr 8 and 255).toDouble() / 256.0
        var b = (rgb and 255).toDouble() / 256.0

        r = r.pow(brightness)
        g = g.pow(brightness)
        b = b.pow(brightness)

        return (((r * 256.0).toInt() shl 16)
                or ((g * 256.0).toInt() shl 8)
                or ((b * 256.0).toInt()))
    }

    @JvmStatic
    fun createPalette(brightness: Double): IntArray {
        val colorPalette = IntArray(65536)
        for (i in colorPalette.indices) {
            colorPalette[i] = HSLtoRGB(i.toShort(), brightness)
        }
        return colorPalette
    }

    fun getRGBFull(hsl: Int): Int {
        return HSLtoRGBFull(hsl)
    }
}