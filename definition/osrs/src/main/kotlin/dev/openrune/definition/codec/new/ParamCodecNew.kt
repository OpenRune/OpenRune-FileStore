package dev.openrune.definition.codec.new

import dev.openrune.definition.opcode.DefinitionOpcode
import dev.openrune.definition.opcode.OpcodeDefinitionCodec
import dev.openrune.definition.opcode.OpcodeList
import dev.openrune.definition.opcode.OpcodeType
import dev.openrune.definition.opcode.impl.DefinitionOpcodeEnum
import dev.openrune.definition.type.ParamType
import dev.openrune.definition.util.VarType

class ParamCodecNew : OpcodeDefinitionCodec<ParamType>() {

    override val definitionCodec = OpcodeList<ParamType>().apply {
        add(DefinitionOpcodeEnum(1, OpcodeType.UBYTE, ParamType::type,
            { idx -> VarType.byChar(idx.toChar()) },
            { it!!.ch.code },
            null
        ))

        add(DefinitionOpcode(2, OpcodeType.INT, ParamType::defaultInt, readOnly = true))
        add(DefinitionOpcode(3, OpcodeType.INT, ParamType::defaultInt))
        add(DefinitionOpcode(4, ParamType::isMembers, setValue = false, encodeWhen = false))
        add(DefinitionOpcode(5, OpcodeType.STRING, ParamType::defaultString))
    }

    override fun createDefinition() = ParamType()
}

