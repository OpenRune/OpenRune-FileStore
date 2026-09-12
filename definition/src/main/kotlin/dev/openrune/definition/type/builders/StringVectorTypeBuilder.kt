package dev.openrune.definition.type.builders

import dev.openrune.definition.type.*

/**
 * The mutable side of [StringVectorType]. Codecs decode into one of these and the packing tools
 * edit one — either fresh or via [StringVectorType.toBuilder] — then [build] produces the
 * immutable definition everything else reads.
 */
class StringVectorTypeBuilder(var id: Int = -1) {

    var persist: Boolean = false

    fun build(): StringVectorType = StringVectorType(
        id = id,
        persist = persist,
    )

    companion object {
        /** Copies every field of [type] into a fresh builder. */
        fun from(type: StringVectorType): StringVectorTypeBuilder {
            val builder = StringVectorTypeBuilder(type.id)
            builder.persist = type.persist
            return builder
        }
    }
}
