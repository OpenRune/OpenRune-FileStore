package dev.openrune.definition.type

import dev.openrune.definition.Definition

data class StringVectorType(
    override var id: Int = -1,
    var persist: Boolean = false
) : Definition