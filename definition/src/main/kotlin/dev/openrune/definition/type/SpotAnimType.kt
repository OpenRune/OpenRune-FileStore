package dev.openrune.definition.type

import dev.openrune.definition.type.builders.SpotAnimTypeBuilder

import dev.openrune.toml.rsconfig.RsTableHeaders
import dev.openrune.definition.Definition
import dev.openrune.definition.Recolourable

@RsTableHeaders("graphics", "graphic")
data class SpotAnimType(
    override var id: Int = -1,
    override val originalColours: List<Int>? = null,
    override val modifiedColours: List<Int>? = null,
    override val originalTextureColours: List<Int>? = null,
    override val modifiedTextureColours: List<Int>? = null,
    val resizeY: Int = 128,
    val resizeX: Int = 128,
    val rotation: Int = 0,
    val rotate : Boolean = true,
    val animationId: Int = -1,
    val modelId: Int = 0,
    val ambient: Int = 0,
    val contrast: Int = 0,
    val debugName : String = ""
) : Definition, Recolourable {

    /** A mutable copy of this definition, for tools that need to edit and re-pack it. */
    fun toBuilder(): SpotAnimTypeBuilder = SpotAnimTypeBuilder.from(this)
}
