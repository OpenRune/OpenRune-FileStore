package dev.openrune.definition.type

import dev.openrune.definition.Definition

data class VarClientType(
    override var id: Int = -1,
    var persist: Boolean = false
) : Definition