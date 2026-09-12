package dev.openrune.definition.type

import dev.openrune.definition.type.builders.VarClientTypeBuilder

import dev.openrune.toml.rsconfig.RsTableHeaders
import dev.openrune.definition.Definition

/**
 * A loaded var client definition. Immutable apart from [id] (which the load machinery assigns):
 * decoding builds one through [VarClientTypeBuilder], and everything after that only reads.
 */
@RsTableHeaders("varclient")
data class VarClientType(
    override var id: Int = -1,
    val persist: Boolean = false
) : Definition {

    /** A mutable copy of this definition, for tools that need to edit and re-pack it. */
    fun toBuilder(): VarClientTypeBuilder = VarClientTypeBuilder.from(this)
}
