package dev.openrune.definition.codec.new

import dev.openrune.definition.opcode.DefinitionOpcode
import dev.openrune.definition.opcode.OpcodeDefinitionCodec
import dev.openrune.definition.opcode.OpcodeList
import dev.openrune.definition.opcode.OpcodeType
import dev.openrune.definition.opcode.impl.DefinitionOpcodeColours
import dev.openrune.definition.opcode.impl.DefinitionOpcodeTextures
import dev.openrune.definition.type.SpotAnimType

class SpotAnimCodecNew : OpcodeDefinitionCodec<SpotAnimType>() {

    override val definitionCodec = OpcodeList<SpotAnimType>().apply {
        add(DefinitionOpcode(1, OpcodeType.USHORT, SpotAnimType::modelId))
        add(DefinitionOpcode(2, OpcodeType.USHORT, SpotAnimType::animationId))
        add(DefinitionOpcode(4, OpcodeType.USHORT, SpotAnimType::resizeX))
        add(DefinitionOpcode(5, OpcodeType.USHORT, SpotAnimType::resizeY))
        add(DefinitionOpcode(6, OpcodeType.USHORT, SpotAnimType::rotation))
        add(DefinitionOpcode(7, OpcodeType.UBYTE, SpotAnimType::ambient))
        add(DefinitionOpcode(8, OpcodeType.UBYTE, SpotAnimType::contrast))
        add(DefinitionOpcode(9, OpcodeType.STRING, SpotAnimType::debugName))
        
        add(DefinitionOpcodeColours(40, SpotAnimType::originalColours, SpotAnimType::modifiedColours))
        add(DefinitionOpcodeTextures(41, SpotAnimType::originalTextureColours, SpotAnimType::modifiedTextureColours))
    }

    override fun createDefinition() = SpotAnimType()
}

