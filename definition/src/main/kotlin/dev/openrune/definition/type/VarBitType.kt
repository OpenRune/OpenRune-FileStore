package dev.openrune.definition.type

import dev.openrune.definition.type.builders.VarBitTypeBuilder

import dev.openrune.toml.rsconfig.RsTableHeaders
import dev.openrune.definition.Definition

@RsTableHeaders("varbit")
data class VarBitType(
    override var id: Int = -1,
    val varp: Int = 0,
    val startBit: Int = 0,
    val endBit: Int = 0,

) : Definition {

    fun toBuilder(): VarBitTypeBuilder = VarBitTypeBuilder.from(this)
}
