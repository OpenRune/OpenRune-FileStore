package dev.openrune.definition.type

import dev.openrune.definition.type.builders.OverlayTypeBuilder

import dev.openrune.toml.rsconfig.RsTableHeaders
import dev.openrune.definition.Definition

@RsTableHeaders("overlay")
data class OverlayType(
    override var id: Int = -1,
    val primaryRgb: Int = 0,
    val secondaryRgb: Int = -1,
    val texture: Int = -1,
    val water: Int = -1,
    val hideUnderlay: Boolean = true,
    val hue: Int = 0,
    val saturation: Int = 0,
    val lightness: Int = 0,
    val secondaryHue: Int = 0,
    val secondarySaturation: Int = 0,
    val secondaryLightness: Int = 0,
) : Definition {

    fun toBuilder(): OverlayTypeBuilder = OverlayTypeBuilder.from(this)
}
