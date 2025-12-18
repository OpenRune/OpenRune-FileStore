package dev.openrune.definition.codec.new

import dev.openrune.definition.opcode.DefinitionOpcode
import dev.openrune.definition.opcode.OpcodeDefinitionCodec
import dev.openrune.definition.opcode.OpcodeList
import dev.openrune.definition.opcode.OpcodeType
import dev.openrune.definition.type.VarpType

class VarCodecNew : OpcodeDefinitionCodec<VarpType>() {

    override val definitionCodec = OpcodeList<VarpType>().apply {
        add(DefinitionOpcode(5, OpcodeType.USHORT, VarpType::configType))
    }

    override fun createDefinition() = VarpType()
}

