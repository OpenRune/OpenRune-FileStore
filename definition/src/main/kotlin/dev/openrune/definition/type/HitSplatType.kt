package dev.openrune.definition.type

import dev.openrune.definition.type.builders.HitSplatTypeBuilder

import dev.openrune.toml.rsconfig.RsTableHeaders
import dev.openrune.definition.Definition
import dev.openrune.definition.Transforms

@RsTableHeaders("hitsplat")
data class HitSplatType(
    override var id: Int = -1,
    val font: Int = -1,
    val textColour: Int = 16777215,
    val icon: Int = -1,
    val left: Int = -1,
    val middle: Int = -1,
    val right: Int = -1,
    val offsetX: Int = 0,
    val amount: String = "",
    val duration: Int = 70,
    val offsetY: Int = 0,
    val fade: Int = -1,
    val comparisonType: Int = -1,
    val damageYOfset: Int = 0,
    override val multiVarBit: Int = -1,
    override val multiVarp: Int = -1,
    override val multiDefault: Int = -1,
    override val transforms: List<Int>? = null,

) : Definition, Transforms {

    /** A mutable copy of this definition, for tools that need to edit and re-pack it. */
    fun toBuilder(): HitSplatTypeBuilder = HitSplatTypeBuilder.from(this)
}
