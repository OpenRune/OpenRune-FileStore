package dev.openrune.definition.type

import dev.openrune.definition.Definition

data class UnderlayType(
    override var id: Int = -1,
    var rgb: Int = 0
) : Definition {

    var hue: Int = 0
    var saturation: Int = 0
    var lightness: Int = 0
    var hueMultiplier: Int = 0
    var rawHue: Int = 0
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
        val lightnessValue = (maxColorValue + minColorValue) / 2.0
        if (minColorValue != maxColorValue) {
            if (lightnessValue < 0.5) {
                saturationValue = (maxColorValue - minColorValue) / (minColorValue + maxColorValue)
            }

            if (lightnessValue >= 0.5) {
                saturationValue = (maxColorValue - minColorValue) / (2.0 - maxColorValue - minColorValue)
            }

            if (red == maxColorValue) {
                hueValue = (green - blue) / (maxColorValue - minColorValue)
            } else if (green == maxColorValue) {
                hueValue = 2.0 + (blue - red) / (maxColorValue - minColorValue)
            } else if (blue == maxColorValue) {
                hueValue = 4.0 + (red - green) / (maxColorValue - minColorValue)
            }
        }

        hueValue /= 6.0
        this.rawHue = (256.0 * hueValue).toInt()
        this.saturation = (256.0 * saturationValue).toInt()
        this.lightness = (256.0 * lightnessValue).toInt()
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

        if (lightnessValue > 0.5) {
            this.hueMultiplier = (512.0 * saturationValue * (1.0 - lightnessValue)).toInt()
        } else {
            this.hueMultiplier = (512.0 * lightnessValue * saturationValue).toInt()
        }

        if (this.hueMultiplier < 1) {
            this.hueMultiplier = 1
        }

        this.hue = (hueMultiplier.toDouble() * hueValue).toInt()
    }


}