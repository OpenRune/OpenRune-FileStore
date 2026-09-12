package dev.openrune.definition.type.builders

import dev.openrune.definition.type.*

class BugTemplateTypeBuilder(var id: Int = -1) {

    fun build(): BugTemplateType = BugTemplateType(
        id = id,
    )

    companion object {
        /** Copies every field of [type] into a fresh builder. */
        fun from(type: BugTemplateType): BugTemplateTypeBuilder {
            return BugTemplateTypeBuilder(type.id)
        }
    }
}
