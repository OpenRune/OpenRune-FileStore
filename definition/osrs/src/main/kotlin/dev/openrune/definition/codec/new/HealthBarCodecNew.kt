package dev.openrune.definition.codec.new

import dev.openrune.definition.opcode.DefinitionOpcode
import dev.openrune.definition.opcode.IgnoreOpcode
import dev.openrune.definition.opcode.OpcodeDefinitionCodec
import dev.openrune.definition.opcode.OpcodeList
import dev.openrune.definition.opcode.OpcodeType
import dev.openrune.definition.type.HealthBarType

class HealthBarCodecNew : OpcodeDefinitionCodec<HealthBarType>() {

    override val definitionCodec = OpcodeList<HealthBarType>().apply {
        add(IgnoreOpcode(1, OpcodeType.USHORT))

        add(DefinitionOpcode(2, OpcodeType.UBYTE, HealthBarType::int1))
        add(DefinitionOpcode(3, OpcodeType.UBYTE, HealthBarType::int2))
        
        add(DefinitionOpcode(4, HealthBarType::int3, setValue = 0))
        
        add(DefinitionOpcode(5, OpcodeType.USHORT, HealthBarType::int4))

        add(IgnoreOpcode(6, OpcodeType.UBYTE))

        add(DefinitionOpcode(7, OpcodeType.USHORT, HealthBarType::frontSpriteId))
        add(DefinitionOpcode(8, OpcodeType.USHORT, HealthBarType::backSpriteId))
        
        add(DefinitionOpcode(11, OpcodeType.USHORT, HealthBarType::int3))
        
        add(DefinitionOpcode(14, OpcodeType.UBYTE, HealthBarType::width))
        add(DefinitionOpcode(15, OpcodeType.UBYTE, HealthBarType::widthPadding))
    }

    override fun createDefinition() = HealthBarType()
}

