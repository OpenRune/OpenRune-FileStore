package dev.openrune.definition.codec.new

import dev.openrune.definition.opcode.DefinitionOpcode
import dev.openrune.definition.opcode.OpcodeDefinitionCodec
import dev.openrune.definition.opcode.OpcodeList
import dev.openrune.definition.opcode.OpcodeType
import dev.openrune.definition.opcode.impl.DefinitionOpcodeColours
import dev.openrune.definition.opcode.impl.DefinitionOpcodeList
import dev.openrune.definition.opcode.impl.DefinitionOpcodeTextures
import dev.openrune.definition.type.IdentityKitType

class IdentityKitCodecNew : OpcodeDefinitionCodec<IdentityKitType>() {

    override val definitionCodec = OpcodeList<IdentityKitType>().apply {
        add(DefinitionOpcode(1, OpcodeType.UBYTE, IdentityKitType::bodyPartId))
        add(DefinitionOpcodeList(2, OpcodeType.USHORT, IdentityKitType::models))
        add(DefinitionOpcode(3, IdentityKitType::nonSelectable, setValue = true))
        add(DefinitionOpcodeColours(40, IdentityKitType::originalColours, IdentityKitType::modifiedColours))
        add(DefinitionOpcodeTextures(41, IdentityKitType::originalTextureColours, IdentityKitType::modifiedTextureColours))

        (0 until 4).forEach { index ->
            val opcode = 60 + index
            add(DefinitionOpcode(
                opcode = opcode,
                decode = { buf, def, _ ->
                    def.chatheadModels[index] = buf.readUnsignedShort()
                },
                encode = { buf, def ->
                    buf.writeShort(def.chatheadModels[index])
                },
                shouldEncode = { it.chatheadModels[index] != -1 }
            ))
        }
    }

    override fun createDefinition() = IdentityKitType()
}

