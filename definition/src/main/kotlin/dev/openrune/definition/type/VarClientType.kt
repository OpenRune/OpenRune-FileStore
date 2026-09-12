package dev.openrune.definition.type

import dev.openrune.definition.type.builders.VarClientTypeBuilder

import dev.openrune.toml.rsconfig.RsTableHeaders
import dev.openrune.definition.Definition

@RsTableHeaders("varclient")
data class VarClientType(
    override var id: Int = -1,
    val persist: Boolean = false
) : Definition {

    fun toBuilder(): VarClientTypeBuilder = VarClientTypeBuilder.from(this)
}
