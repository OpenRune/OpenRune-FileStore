package dev.openrune.definition.codec

import dev.openrune.definition.Definition
import dev.openrune.definition.DefinitionCodec
import dev.openrune.definition.Rs2OpcodeOrdered
import io.netty.buffer.ByteBuf

interface Rs2OrderedCodec<T> : DefinitionCodec<T> where T : Definition, T : Rs2OpcodeOrdered {

    fun ByteBuf.writePayload(opcode: Int, occurrence: Int, definition: T)

    override fun readLoop(definition: T, buffer: ByteBuf) {
        definition.opcodeOrder += readOpcodeOrder(buffer) { opcode -> definition.read(opcode, buffer) }
    }

    override fun ByteBuf.encode(definition: T) {
        replayOpcodes(definition.opcodeOrder) { opcode, occurrence ->
            writePayload(opcode, occurrence, definition)
        }
    }
}
