package dev.openrune.definition.type

import dev.openrune.definition.Definition
import dev.openrune.definition.Rs2Opcode
import dev.openrune.definition.Rs2OpcodeOrdered
import dev.openrune.definition.util.CacheVarLiteral

data class Rs2DbColumn(
    var column: Int = 0,
    var types: MutableList<CacheVarLiteral> = mutableListOf(),
    var values: MutableList<Any> = mutableListOf(),
    var hasDefault: Boolean = false,
)

data class Rs2DbRowType(
    override var id: Int = -1,
    var leading: Int? = null,
    var columns: MutableList<Rs2DbColumn> = mutableListOf(),
    var table: Int? = null,
    var pluginApi: Boolean = true,
    override val opcodeOrder: MutableList<Rs2Opcode> = mutableListOf(),
) : Definition, Rs2OpcodeOrdered

data class Rs2DbTableType(
    override var id: Int = -1,
    var columnCount: Int = 0,
    var columns: MutableList<Rs2DbColumn> = mutableListOf(),
    var sizePrefixed: Boolean = false,
    override val opcodeOrder: MutableList<Rs2Opcode> = mutableListOf(),
) : Definition, Rs2OpcodeOrdered
