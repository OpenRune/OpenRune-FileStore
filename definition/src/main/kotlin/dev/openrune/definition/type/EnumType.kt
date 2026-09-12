package dev.openrune.definition.type

import dev.openrune.definition.type.builders.EnumTypeBuilder

import dev.openrune.toml.rsconfig.RsTableHeaders
import dev.openrune.toml.serialization.TomlField
import dev.openrune.definition.Definition
import dev.openrune.definition.util.CacheVarLiteral
import dev.openrune.seralizer.CacheVarLiteralSeralizier
import dev.openrune.seralizer.EnumTypeTableHook
import dev.openrune.seralizer.ParamSerializer

/**
 * A loaded enum definition. Immutable apart from [id]: decoding and packing build one through
 * [EnumTypeBuilder]. [values] stays declared MutableMap because ParamSerializer's type argument
 * must match, but by convention it is never modified after construction.
 */
@RsTableHeaders(
    "enum",
    rowPostDecode = EnumTypeTableHook::class,
)
data class EnumType(
    override var id: Int = -1,
    @param:TomlField(serializer = CacheVarLiteralSeralizier::class)
    val keyType: CacheVarLiteral = CacheVarLiteral.INT,
    @param:TomlField(serializer = CacheVarLiteralSeralizier::class)
    val valueType: CacheVarLiteral = CacheVarLiteral.INT,
    val defaultInt: Int = 0,
    val defaultString: String = "",
    @param:TomlField(serializer = ParamSerializer::class)
    val values: MutableMap<Int, Any> = HashMap()
) : Definition {

    /** A mutable copy of this definition, for tools that need to edit and re-pack it. */
    fun toBuilder(): EnumTypeBuilder = EnumTypeBuilder.from(this)

    fun getSize() = values.size

    fun getInt(key: Int): Int = values.get(key) as? Int ?: defaultInt

    fun getString(key: Int): String = values.get(key) as? String ?: defaultString
}
