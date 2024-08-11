package dev.openrune.cache.filestore.definition.data

import dev.openrune.cache.filestore.definition.Definition

import kotlinx.serialization.Serializable

@Serializable
data class VarBitType(
    override var id: Int = -1,
    var varp: Int = 0,
    var startBit: UByte = 0u,
    var endBit: UByte = 0u,
    //Custom
    override var inherit: Int = -1
) : Definition