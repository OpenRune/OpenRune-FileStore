package dev.openrune.definition.type

import dev.openrune.definition.Definition

data class Rs2MaterialType(
    override var id: Int = -1,
    var version: Int = -1,
    var diffuseAlpha: Int = -1,
    var diffuseAlphaSize: Int = 0,
    var normalEmissive: Int = -1,
    var normalEmissiveSize: Int = 0,
    var metalnessRoughness: Int = -1,
    var metalnessRoughnessSize: Int = 0,
    var environment: Int = -1,
    var environmentSize: Int = 0,
    var repeatU: Int = 0,
    var repeatV: Int = 0,
    var normal: Boolean = false,
    var facetMode: Int = 0,
    var qualityMode: Int = -1,
    var alphaMode: Int = 0,
    var alphaTest: Int = -1,
    var speedU: Int = 0,
    var speedV: Int = 0,
    var effect: Int = -1,
    var mipmapping: Int = -1,
    var lightness: Int = 0,
    var saturation: Int = 0,
    var averageColour: Int = 0,
    var size: Int = 0,
) : Definition {

    val rt7: Boolean get() = version != 0

    val textures: List<Int>
        get() = listOf(diffuseAlpha, normalEmissive, metalnessRoughness, environment).filter { it != -1 }
}
