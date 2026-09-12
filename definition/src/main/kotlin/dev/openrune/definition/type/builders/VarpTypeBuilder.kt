package dev.openrune.definition.type.builders

import dev.openrune.definition.type.*

/**
 * The mutable side of [VarpType]. Codecs decode into one of these and the packing tools edit
 * one — either fresh or via [VarpType.toBuilder] — then [build] produces the immutable
 * definition everything else reads.
 */
class VarpTypeBuilder(var id: Int = -1) {

    var configType: Int = 0

    fun build(): VarpType = VarpType(
        id = id,
        configType = configType,
    )

    companion object {
        /** Copies every field of [type] into a fresh builder. */
        fun from(type: VarpType): VarpTypeBuilder {
            val builder = VarpTypeBuilder(type.id)
            builder.configType = type.configType
            return builder
        }
    }
}
