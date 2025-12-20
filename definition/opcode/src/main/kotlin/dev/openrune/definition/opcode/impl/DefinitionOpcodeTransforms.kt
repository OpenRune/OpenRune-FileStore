package dev.openrune.definition.opcode.impl

import dev.openrune.definition.Transforms
import dev.openrune.definition.opcode.DefinitionOpcode
import io.netty.buffer.ByteBuf

fun <T : Transforms> DefinitionOpcodeTransforms(
    opcodes: IntRange,
    extendedTransforms: Boolean = false
): DefinitionOpcode<T> = DefinitionOpcode(
    opcodes,
    decode = { buf, def, opcode ->
        def.readTransforms(buf, opcode == opcodes.last, extendedTransforms)
    },
    encode = { buf, def ->
        def.writeTransforms(buf, opcodes.first, opcodes.last)
    },
    skipByteEncode = true,
    shouldEncode = { true }
)