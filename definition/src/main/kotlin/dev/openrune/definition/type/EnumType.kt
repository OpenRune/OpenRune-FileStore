package dev.openrune.definition.type

import dev.openrune.definition.type.builders.EnumTypeBuilder

import dev.openrune.toml.rsconfig.RsTableHeaders
import dev.openrune.toml.serialization.TomlField
import dev.openrune.definition.Definition
import dev.openrune.definition.Rs2Opcode
import dev.openrune.definition.Rs2OpcodeOrdered
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
    val keyType: CacheVarLiteral = CacheVarLiteral.INT,
    @param:TomlField(serializer = CacheVarLiteralSeralizier::class)
    val valueType: CacheVarLiteral = CacheVarLiteral.INT,
    val defaultInt: Int = 0,
    val defaultLong: Long = 0L,
    val defaultString: String = "",
    @param:TomlField(serializer = ParamSerializer::class)
    val values: MutableMap<Int, Any> = LinkedHashMap(),
) : Definition, Rs2OpcodeOrdered {

    override val opcodeOrder: MutableList<Rs2Opcode> = mutableListOf()

    var rs2: Rs2? = null

    data class Rs2(
        val dense: Boolean = false,
        val capacity: Int? = null,
        val typesFromId: Boolean = false,
        val pluginApi: Boolean = true,
    )

    fun toBuilder(): EnumTypeBuilder = EnumTypeBuilder.from(this)

    fun getSize() = values.size

    fun getInt(key: Int): Int = values.get(key) as? Int ?: defaultInt

    fun getLong(key: Int): Long = values.get(key) as? Long ?: defaultLong

    fun getString(key: Int): String = values.get(key) as? String ?: defaultString
}
