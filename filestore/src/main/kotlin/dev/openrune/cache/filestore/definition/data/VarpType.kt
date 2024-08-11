package dev.openrune.cache.filestore.definition.data

import dev.openrune.cache.filestore.definition.Definition

import kotlinx.serialization.Serializable

@Serializable
data class VarpType(
    override var id: Int = -1,
    var configType: UShort = 0u,
    //Custom
    override var inherit: Int = -1
) : Definition