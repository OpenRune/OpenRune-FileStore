package dev.openrune.definition.type

import dev.openrune.definition.type.builders.BugTemplateTypeBuilder

import dev.openrune.definition.Definition

/**
 * A loaded bug template definition. Immutable apart from [id] (which the load machinery
 * assigns): decoding builds one through [BugTemplateTypeBuilder], and everything after that
 * only reads.
 */
data class BugTemplateType(
    override var id: Int = -1,
) : Definition {

    /** A mutable copy of this definition, for tools that need to edit and re-pack it. */
    fun toBuilder(): BugTemplateTypeBuilder = BugTemplateTypeBuilder.from(this)
}
