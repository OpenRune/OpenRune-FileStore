package dev.openrune.definition.type

import dev.openrune.definition.Definition
import kotlinx.serialization.Serializable

@Serializable
data class VarpType(
    override var id: Int = -1,
    var configType: Int = 0,

) : Definition