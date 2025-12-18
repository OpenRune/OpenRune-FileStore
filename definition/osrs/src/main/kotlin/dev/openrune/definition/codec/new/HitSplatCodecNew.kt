package dev.openrune.definition.codec.new

import dev.openrune.definition.opcode.DefinitionOpcode
import dev.openrune.definition.opcode.OpcodeDefinitionCodec
import dev.openrune.definition.opcode.OpcodeList
import dev.openrune.definition.opcode.OpcodeType
import dev.openrune.definition.opcode.impl.DefinitionOpcodeTransforms
import dev.openrune.definition.type.HitSplatType

class HitSplatCodecNew : OpcodeDefinitionCodec<HitSplatType>() {

    override val definitionCodec = OpcodeList<HitSplatType>().apply {
        add(DefinitionOpcode(1, OpcodeType.NULLABLE_LARGE_SMART, HitSplatType::font))
        add(DefinitionOpcode(2, OpcodeType.UMEDIUM, HitSplatType::textColour))
        add(DefinitionOpcode(3, OpcodeType.NULLABLE_LARGE_SMART, HitSplatType::icon))
        add(DefinitionOpcode(4, OpcodeType.NULLABLE_LARGE_SMART, HitSplatType::left))
        add(DefinitionOpcode(5, OpcodeType.NULLABLE_LARGE_SMART, HitSplatType::middle))
        add(DefinitionOpcode(6, OpcodeType.NULLABLE_LARGE_SMART, HitSplatType::right))
        
        add(DefinitionOpcode(7, OpcodeType.USHORT, HitSplatType::offsetX))
        add(DefinitionOpcode(9, OpcodeType.USHORT, HitSplatType::duration))
        add(DefinitionOpcode(10, OpcodeType.SHORT, HitSplatType::offsetY))
        add(DefinitionOpcode(12, OpcodeType.UBYTE, HitSplatType::comparisonType))
        add(DefinitionOpcode(13, OpcodeType.SHORT, HitSplatType::damageYOfset))
        
        add(DefinitionOpcode(8, OpcodeType.PREFIXED_STRING, HitSplatType::amount))
        
        add(DefinitionOpcode(11, HitSplatType::fade, setValue = 0))
        
        add(DefinitionOpcode(14, OpcodeType.SHORT, HitSplatType::fade))
        
        add(DefinitionOpcodeTransforms(17..18, HitSplatType::transforms, HitSplatType::varbit, HitSplatType::varp))
    }

    override fun createDefinition() = HitSplatType()
}
