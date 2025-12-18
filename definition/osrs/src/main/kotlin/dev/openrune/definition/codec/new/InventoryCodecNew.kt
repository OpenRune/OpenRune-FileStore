package dev.openrune.definition.codec.new

import dev.openrune.definition.opcode.DefinitionOpcode
import dev.openrune.definition.opcode.OpcodeDefinitionCodec
import dev.openrune.definition.opcode.OpcodeList
import dev.openrune.definition.opcode.OpcodeType
import dev.openrune.definition.type.InventoryType

class InventoryCodecNew : OpcodeDefinitionCodec<InventoryType>() {

    override val definitionCodec = OpcodeList<InventoryType>().apply {
        add(DefinitionOpcode(2, OpcodeType.USHORT, InventoryType::size))
    }

    override fun createDefinition() = InventoryType()
}

