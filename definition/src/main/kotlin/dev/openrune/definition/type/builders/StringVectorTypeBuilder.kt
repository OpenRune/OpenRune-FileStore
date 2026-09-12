package dev.openrune.definition.type.builders

import dev.openrune.definition.type.*

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
