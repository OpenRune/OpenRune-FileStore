package dev.openrune.definition.codec.new

import dev.openrune.definition.opcode.DefinitionOpcode
import dev.openrune.definition.opcode.IgnoreOpcode
import dev.openrune.definition.opcode.OpcodeDefinitionCodec
import dev.openrune.definition.opcode.OpcodeList
import dev.openrune.definition.opcode.OpcodeType
import dev.openrune.definition.opcode.impl.DefinitionOpcodeEnum
import dev.openrune.definition.opcode.impl.DefinitionOpcodeListActions
import dev.openrune.definition.type.WorldEntityType
import dev.openrune.definition.type.WorldInteractMode
import dev.openrune.definition.type.WorldInteractTarget

class WorldEntityCodecNew : OpcodeDefinitionCodec<WorldEntityType>() {

    override val definitionCodec = OpcodeList<WorldEntityType>().apply {
        add(DefinitionOpcode(2, OpcodeType.UBYTE, WorldEntityType::mainLevel))
        add(DefinitionOpcode(4, OpcodeType.SHORT, WorldEntityType::mainX))
        add(DefinitionOpcode(5, OpcodeType.SHORT, WorldEntityType::mainZ))
        add(DefinitionOpcode(6, OpcodeType.SHORT, WorldEntityType::boundsOffsetX))
        add(DefinitionOpcode(7, OpcodeType.SHORT, WorldEntityType::boundsOffsetZ))
        add(DefinitionOpcode(8, OpcodeType.USHORT, WorldEntityType::boundSizeZ))
        add(DefinitionOpcode(9, OpcodeType.USHORT, WorldEntityType::boundsSizeZ))
        add(DefinitionOpcode(12, OpcodeType.STRING, WorldEntityType::name))
        add(DefinitionOpcode(14, WorldEntityType::active, setValue = true))
        
        addAll(DefinitionOpcodeListActions(15..19, WorldEntityType::options,
            customSetter = { def, list ->
                def.options = list.toMutableList()
                def.active = true
            }
        ))
        
        add(IgnoreOpcode(20, OpcodeType.USHORT))
        
        add(DefinitionOpcodeEnum(23, OpcodeType.UBYTE, WorldEntityType::interactTarget,
            WorldInteractTarget::fromId, { it.id }, WorldInteractTarget.UNKNOWN))

        add(DefinitionOpcodeEnum(24, OpcodeType.UBYTE, WorldEntityType::interactContentsMode,
            WorldInteractMode::fromId, { it.id }, WorldInteractMode.UNKNOWN))
        
        add(DefinitionOpcode(25, OpcodeType.USHORT, WorldEntityType::anim))
        add(DefinitionOpcode(26, OpcodeType.NULLABLE_LARGE_SMART, WorldEntityType::minimapIcon))
        add(DefinitionOpcode(27, OpcodeType.USHORT, WorldEntityType::rgb))
    }

    override fun createDefinition() = WorldEntityType()
}

