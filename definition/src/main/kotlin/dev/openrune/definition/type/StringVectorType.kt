package dev.openrune.definition.type

import dev.openrune.definition.type.builders.StringVectorTypeBuilder

import dev.openrune.definition.Definition

data class StringVectorType(
    override var id: Int = -1,
    val persist: Boolean = false
) : Definition {

    fun toBuilder(): StringVectorTypeBuilder = StringVectorTypeBuilder.from(this)
}
