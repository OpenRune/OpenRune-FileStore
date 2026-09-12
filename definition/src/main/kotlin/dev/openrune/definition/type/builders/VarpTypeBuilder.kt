package dev.openrune.definition.type.builders

import dev.openrune.definition.type.*

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
