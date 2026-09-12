package dev.openrune.definition.type.builders

import dev.openrune.definition.type.*

import dev.openrune.definition.MutableParameterized

/**
 * The mutable side of [InventoryType]. Codecs decode into one of these and the packing tools edit
 * one — either fresh or via [InventoryType.toBuilder] — then [build] produces the immutable
 * definition everything else reads.
 */
class InventoryTypeBuilder(var id: Int = -1) : MutableParameterized {

    var size: Int = 0

    override var params: MutableMap<Int, Any>? = null

    fun build(): InventoryType = InventoryType(
        id = id,
        size = size,
        params = params,
    )

    companion object {
        /** Copies every field of [type] into a fresh builder. Map contents are copied too. */
        fun from(type: InventoryType): InventoryTypeBuilder {
            val builder = InventoryTypeBuilder(type.id)
            builder.size = type.size
            builder.params = type.params?.toMutableMap()
            return builder
        }
    }
}
