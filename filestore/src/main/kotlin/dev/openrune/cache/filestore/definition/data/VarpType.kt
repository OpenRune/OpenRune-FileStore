package dev.openrune.cache.filestore.definition.data

import dev.openrune.cache.filestore.definition.Definition

import kotlinx.serialization.Serializable

@Serializable
data class VarpType(
    override var id: Int = -1,
    var configType: Int = 0,
    //Custom
    override var inherit: Int = -1
) : Definition