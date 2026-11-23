package dev.openrune.definition.type

import dev.openrune.definition.type.builders.ParamTypeBuilder

import dev.openrune.toml.rsconfig.RsTableHeaders
import dev.openrune.toml.serialization.TomlField
import dev.openrune.definition.Definition
import dev.openrune.definition.Rs2Opcode
import dev.openrune.definition.Rs2OpcodeOrdered
import dev.openrune.definition.util.CacheVarLiteral
import dev.openrune.seralizer.CacheVarLiteralSeralizier
import dev.openrune.seralizer.ParamTypeTableHook

@RsTableHeaders(
    "params",
    rowPostDecode = ParamTypeTableHook::class,
)
data class ParamType(
    override var id: Int = -1,
    @param:TomlField(serializer = CacheVarLiteralSeralizier::class)
    val type: CacheVarLiteral? = null,
    val isMembers: Boolean = true,
    val defaultInt: Int = 0,
    val defaultString: String? = null,
    val defaultLong: Long = 0L,
) : Definition, Rs2OpcodeOrdered {

    override val opcodeOrder: MutableList<Rs2Opcode> = mutableListOf()

    var rs2: Rs2? = null

    data class Rs2(
        val typeFromId: Boolean = false,
    )

    fun toBuilder(): ParamTypeBuilder = ParamTypeBuilder.from(this)
}