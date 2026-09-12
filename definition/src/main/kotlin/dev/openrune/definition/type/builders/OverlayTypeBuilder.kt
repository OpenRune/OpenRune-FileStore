package dev.openrune.definition.type.builders

import dev.openrune.definition.type.*

class OverlayTypeBuilder(var id: Int = -1) {

    var primaryRgb: Int = 0
    var secondaryRgb: Int = -1
    var texture: Int = -1
    var water: Int = -1
    var hideUnderlay: Boolean = true

    fun build(): OverlayType {
        var secondaryHue = 0
        var secondarySaturation = 0
        var secondaryLightness = 0

        if (secondaryRgb != -1) {
            val (h, s, l) = hsl(secondaryRgb)
            secondaryHue = h
            secondarySaturation = s
            secondaryLightness = l
        }

        val (hue, saturation, lightness) = hsl(primaryRgb)

        return OverlayType(
            id = id,
            primaryRgb = primaryRgb,
            secondaryRgb = secondaryRgb,
            texture = texture,
            water = water,
            hideUnderlay = hideUnderlay,
            hue = hue,
            saturation = saturation,
            lightness = lightness,
            secondaryHue = secondaryHue,
            secondarySaturation = secondarySaturation,
            secondaryLightness = secondaryLightness,
        )
    }

    companion object {
        fun from(type: OverlayType): OverlayTypeBuilder {
            val builder = OverlayTypeBuilder(type.id)
            builder.primaryRgb = type.primaryRgb
            builder.secondaryRgb = type.secondaryRgb
            builder.texture = type.texture
            builder.water = type.water
            builder.hideUnderlay = type.hideUnderlay
            return builder
        }

        internal fun hsl(rgbValue: Int): Triple<Int, Int, Int> {
            val red = (rgbValue shr 16 and 255).toDouble() / 256.0
            val green = (rgbValue shr 8 and 255).toDouble() / 256.0
            val blue = (rgbValue and 255).toDouble() / 256.0
            var minColorValue = red
            if (green < red) {
                minColorValue = green
            }

            if (blue < minColorValue) {
                minColorValue = blue
            }

            var maxColorValue = red
            if (green > red) {
                maxColorValue = green
            }

            if (blue > maxColorValue) {
                maxColorValue = blue
            }

            var hueValue = 0.0
            var saturationValue = 0.0
            val lightnessValue = (minColorValue + maxColorValue) / 2.0
            if (minColorValue != maxColorValue) {
                if (lightnessValue < 0.5) {
                    saturationValue = (maxColorValue - minColorValue) / (minColorValue + maxColorValue)
                }

                if (lightnessValue >= 0.5) {
                    saturationValue = (maxColorValue - minColorValue) / (2.0 - maxColorValue - minColorValue)
                }

                if (red == maxColorValue) {
                    hueValue = (green - blue) / (maxColorValue - minColorValue)
                } else if (maxColorValue == green) {
                    hueValue = 2.0 + (blue - red) / (maxColorValue - minColorValue)
                } else if (blue == maxColorValue) {
                    hueValue = (red - green) / (maxColorValue - minColorValue) + 4.0
                }
            }

            hueValue /= 6.0
            val hue = (hueValue * 256.0).toInt()
            val saturation = ((saturationValue * 256.0).toInt()).coerceIn(0, 255)
            val lightness = ((lightnessValue * 256.0).toInt()).coerceIn(0, 255)
            return Triple(hue, saturation, lightness)
        }
    }
}
