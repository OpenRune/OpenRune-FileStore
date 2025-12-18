package dev.openrune.definition.codec.new

import dev.openrune.definition.opcode.OpcodeDefinitionCodec
import dev.openrune.definition.opcode.OpcodeList
import dev.openrune.definition.opcode.impl.DefinitionOpcodeParams
import dev.openrune.definition.type.StructType

class StructCodecNew : OpcodeDefinitionCodec<StructType>() {

    override val definitionCodec = OpcodeList<StructType>().apply {
        add(DefinitionOpcodeParams(249, StructType::params))
    }

    override fun createDefinition() = StructType()
}

