package dev.openrune.definition.type

import dev.openrune.toml.rsconfig.RsTableHeaders
import dev.openrune.definition.Definition

@RsTableHeaders("varclient")
data class VarClientType(
    override var id: Int = -1,
    var persist: Boolean = false
) : Definition