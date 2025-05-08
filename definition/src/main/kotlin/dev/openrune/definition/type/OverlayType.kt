package dev.openrune.definition.type

import dev.openrune.definition.Definition

data class OverlayType(
    override var id: Int = -1,
    var primaryRgb: Int = 0,
    var secondaryRgb: Int = -1,
    var texture: Int = -1,
    var hideUnderlay: Boolean = true
) : Definition {

    var hue: Int = 0
    var saturation: Int = 0
    var lightness: Int = 0
    var secondaryHue: Int = 0
    var secondarySaturation: Int = 0
    var secondaryLightness: Int = 0

    fun calculateHsl() {
        if (this.secondaryRgb != -1) {
            this.setHsl(this.secondaryRgb)
            this.secondaryHue = this.hue
            this.secondarySaturation = this.saturation
            this.secondaryLightness = this.lightness
        }

        this.setHsl(this.primaryRgb)
    }

    fun setHsl(rgbValue: Int) {
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
        this.hue = (hueValue * 256.0).toInt()
        this.saturation = (saturationValue * 256.0).toInt()
        this.lightness = (lightnessValue * 256.0).toInt()
        if (this.saturation < 0) {
            this.saturation = 0
        } else if (this.saturation > 255) {
            this.saturation = 255
        }

        if (this.lightness < 0) {
            this.lightness = 0
        } else if (this.lightness > 255) {
            this.lightness = 255
        }
    }
}