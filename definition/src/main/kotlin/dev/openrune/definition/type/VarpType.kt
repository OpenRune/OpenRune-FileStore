package dev.openrune.definition.type

import dev.openrune.toml.rsconfig.RsTableHeaders
import dev.openrune.definition.Definition

@RsTableHeaders("varp")
data class VarpType(
    override var id: Int = -1,
    var configType: Int = 0,

) : Definition