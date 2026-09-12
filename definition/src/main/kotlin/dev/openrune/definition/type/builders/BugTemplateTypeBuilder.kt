package dev.openrune.definition.type.builders

import dev.openrune.definition.type.*

/**
 * The mutable side of [BugTemplateType]. Codecs decode into one of these and the packing tools
 * edit one — either fresh or via [BugTemplateType.toBuilder] — then [build] produces the
 * immutable definition everything else reads.
 */
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
