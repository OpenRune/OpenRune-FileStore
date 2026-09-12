package dev.openrune.definition.type

import dev.openrune.definition.type.builders.StringVectorTypeBuilder

import dev.openrune.definition.Definition

/**
 * A loaded string vector definition. Immutable apart from [id] (which the load machinery
 * assigns): decoding builds one through [StringVectorTypeBuilder], and everything after that
 * only reads.
 */
data class StringVectorType(
    override var id: Int = -1,
    val persist: Boolean = false
) : Definition {

    /** A mutable copy of this definition, for tools that need to edit and re-pack it. */
    fun toBuilder(): StringVectorTypeBuilder = StringVectorTypeBuilder.from(this)
}
