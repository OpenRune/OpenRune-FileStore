package dev.openrune.definition.codec.new

import dev.openrune.definition.opcode.DefinitionOpcode
import dev.openrune.definition.opcode.OpcodeDefinitionCodec
import dev.openrune.definition.opcode.OpcodeList
import dev.openrune.definition.type.VarClientType

class VarClientCodecNew : OpcodeDefinitionCodec<VarClientType>() {

    override val definitionCodec = OpcodeList<VarClientType>().apply {
        add(DefinitionOpcode(2, VarClientType::persist))
    }

    override fun createDefinition() = VarClientType()
}

