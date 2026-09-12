package dev.openrune.definition.type.builders

import dev.openrune.definition.type.*

import dev.openrune.definition.MutableParameterized

class StructTypeBuilder(var id: Int = -1) : MutableParameterized {

    override var params: MutableMap<Int, Any>? = null

    fun build(): StructType = StructType(
        id = id,
        params = params,
    )

    companion object {
        /** Copies every field of [type] into a fresh builder. Map contents are copied too. */
        fun from(type: StructType): StructTypeBuilder {
            val builder = StructTypeBuilder(type.id)
            builder.params = type.params?.toMutableMap()
            return builder
        }
    }
}
