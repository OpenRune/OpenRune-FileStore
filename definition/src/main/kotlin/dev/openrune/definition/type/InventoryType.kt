package dev.openrune.definition.type

import dev.openrune.toml.rsconfig.RsTableHeaders
import dev.openrune.toml.serialization.TomlField
import dev.openrune.definition.Definition
import dev.openrune.definition.Parameterized
import dev.openrune.seralizer.ParamSerializer

@RsTableHeaders("inventory")
data class InventoryType(
    override var id: Int = -1,
    var size: Int = 0,
    @param:TomlField(serializer = ParamSerializer::class)
    override var params: MutableMap<Int, Any>? = null,
) : Parameterized,Definition