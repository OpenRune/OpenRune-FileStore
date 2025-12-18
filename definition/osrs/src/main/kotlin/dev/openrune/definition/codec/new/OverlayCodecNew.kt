package dev.openrune.definition.codec.new

import dev.openrune.definition.opcode.DefinitionOpcode
import dev.openrune.definition.opcode.OpcodeDefinitionCodec
import dev.openrune.definition.opcode.OpcodeList
import dev.openrune.definition.opcode.OpcodeType
import dev.openrune.definition.type.OverlayType

class OverlayCodecNew : OpcodeDefinitionCodec<OverlayType>() {

    override val definitionCodec = OpcodeList<OverlayType>().apply {
        add(DefinitionOpcode(1, OpcodeType.UMEDIUM, OverlayType::primaryRgb))
        add(DefinitionOpcode(2, OpcodeType.UBYTE, OverlayType::texture))
        add(DefinitionOpcode(5, OverlayType::hideUnderlay, setValue = false))
        add(DefinitionOpcode(7, OpcodeType.UMEDIUM, OverlayType::secondaryRgb))
        add(DefinitionOpcode(9, OpcodeType.UBYTE, OverlayType::water))
    }

    override fun createDefinition() = OverlayType()
}

