package dev.openrune.definition.codec.new

import dev.openrune.definition.opcode.DefinitionOpcode
import dev.openrune.definition.opcode.OpcodeDefinitionCodec
import dev.openrune.definition.opcode.OpcodeList
import dev.openrune.definition.opcode.OpcodeType
import dev.openrune.definition.type.UnderlayType

class UnderlayCodecNew : OpcodeDefinitionCodec<UnderlayType>() {

    override val definitionCodec = OpcodeList<UnderlayType>().apply {
        add(DefinitionOpcode(1, OpcodeType.UMEDIUM, UnderlayType::rgb))
    }

    override fun createDefinition() = UnderlayType()
}

