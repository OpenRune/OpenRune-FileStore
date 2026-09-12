package dev.openrune.definition.type.builders

import dev.openrune.definition.type.*

/**
 * The mutable side of [VarBitType]. Codecs decode into one of these and the packing tools edit
 * one — either fresh or via [VarBitType.toBuilder] — then [build] produces the immutable
 * definition everything else reads.
 */
class VarBitTypeBuilder(var id: Int = -1) {

    var varp: Int = 0
    var startBit: Int = 0
    var endBit: Int = 0

    fun build(): VarBitType = VarBitType(
        id = id,
        varp = varp,
        startBit = startBit,
        endBit = endBit,
    )

    companion object {
        /** Copies every field of [type] into a fresh builder. */
        fun from(type: VarBitType): VarBitTypeBuilder {
            val builder = VarBitTypeBuilder(type.id)
            builder.varp = type.varp
            builder.startBit = type.startBit
            builder.endBit = type.endBit
            return builder
        }
    }
}
