package dev.openrune.definition.type

import dev.openrune.definition.type.builders.VarpTypeBuilder

import dev.openrune.toml.rsconfig.RsTableHeaders
import dev.openrune.definition.Definition

@RsTableHeaders("varp")
data class VarpType(
    override var id: Int = -1,
    val configType: Int = 0,

) : Definition {

    /** A mutable copy of this definition, for tools that need to edit and re-pack it. */
    fun toBuilder(): VarpTypeBuilder = VarpTypeBuilder.from(this)
}
