package dev.openrune.definition.type

import dev.openrune.definition.type.builders.VarClientTypeBuilder

import dev.openrune.toml.rsconfig.RsTableHeaders
import dev.openrune.definition.Definition
import dev.openrune.definition.Rs2Opcode
import dev.openrune.definition.Rs2OpcodeOrdered
import dev.openrune.definition.util.CacheVarLiteral

@RsTableHeaders("varclient")
data class VarClientType(
    override var id: Int = -1,
    val persist: Boolean = false,
) : Definition, Rs2OpcodeOrdered {

    override val opcodeOrder: MutableList<Rs2Opcode> = mutableListOf()

    var rs2: Rs2? = null

    data class Rs2(
        val type: CacheVarLiteral? = null,
    )

    fun toBuilder(): VarClientTypeBuilder = VarClientTypeBuilder.from(this)
}
