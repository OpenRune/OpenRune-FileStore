package dev.openrune.definition.type.builders

import dev.openrune.definition.type.*

import dev.openrune.definition.util.CacheVarLiteral

/**
 * The mutable side of [VarClanType]. Codecs decode into one of these and the packing tools edit
 * one — either fresh or via [VarClanType.toBuilder] — then [build] produces the immutable
 * definition everything else reads.
 */
class VarClanTypeBuilder(var id: Int = -1) {

    var type: CacheVarLiteral? = null
    var lifetime: Int = 0
    var debugName: String = ""

    fun build(): VarClanType = VarClanType(
        id = id,
        type = type,
        lifetime = lifetime,
        debugName = debugName,
    )

    companion object {
        /** Copies every field of [type] into a fresh builder. */
        fun from(type: VarClanType): VarClanTypeBuilder {
            val builder = VarClanTypeBuilder(type.id)
            builder.type = type.type
            builder.lifetime = type.lifetime
            builder.debugName = type.debugName
            return builder
        }
    }
}
