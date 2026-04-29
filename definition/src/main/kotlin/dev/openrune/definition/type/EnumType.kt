package dev.openrune.definition.type

import dev.openrune.toml.rsconfig.RsTableHeaders
import dev.openrune.toml.serialization.TomlField
import dev.openrune.definition.Definition
import dev.openrune.definition.util.CacheVarLiteral
import dev.openrune.seralizer.CacheVarLiteralSeralizier
import dev.openrune.seralizer.EnumTypeTableHook
import dev.openrune.seralizer.ParamSerializer

@RsTableHeaders(
    "enum",
    rowPostDecode = EnumTypeTableHook::class,
)
data class EnumType(
    override var id: Int = -1,
    @param:TomlField(serializer = CacheVarLiteralSeralizier::class)
    var keyType: CacheVarLiteral = CacheVarLiteral.INT,
    @param:TomlField(serializer = CacheVarLiteralSeralizier::class)
    var valueType: CacheVarLiteral = CacheVarLiteral.INT,
    var defaultInt: Int = 0,
    var defaultString: String = "",
    @param:TomlField(serializer = ParamSerializer::class)
    val values: MutableMap<Int, Any> = HashMap()
) : Definition {

    fun getSize() = values.size

    fun getInt(key: Int): Int = values.get(key) as? Int ?: defaultInt

    fun getString(key: Int): String = values.get(key) as? String ?: defaultString
}