package dev.openrune.definition.type

import dev.openrune.definition.type.builders.BugTemplateTypeBuilder

import dev.openrune.definition.Definition

data class BugTemplateType(
    override var id: Int = -1,
) : Definition {

    fun toBuilder(): BugTemplateTypeBuilder = BugTemplateTypeBuilder.from(this)
}
