package dev.openrune.definition.type.builders

import dev.openrune.definition.type.*

/**
 * The mutable side of [VarClientType]. Codecs decode into one of these and the packing tools
 * edit one — either fresh or via [VarClientType.toBuilder] — then [build] produces the immutable
 * definition everything else reads.
 */
class VarClientTypeBuilder(var id: Int = -1) {

    var persist: Boolean = false

    fun build(): VarClientType = VarClientType(
        id = id,
        persist = persist,
    )

    companion object {
        /** Copies every field of [type] into a fresh builder. */
        fun from(type: VarClientType): VarClientTypeBuilder {
            val builder = VarClientTypeBuilder(type.id)
            builder.persist = type.persist
            return builder
        }
    }
}
