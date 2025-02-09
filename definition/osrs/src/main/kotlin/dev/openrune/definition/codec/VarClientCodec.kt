package dev.openrune.definition.codec

import dev.openrune.definition.DefinitionCodec
import dev.openrune.definition.type.VarClientType
import io.netty.buffer.ByteBuf

class VarClientCodec : DefinitionCodec<VarClientType> {
    override fun VarClientType.read(opcode: Int, buffer: ByteBuf) {
        when(opcode) {
            2 -> persist = true
        }
    }

    override fun ByteBuf.encode(definition: VarClientType) {
        if (definition.persist) {
            writeByte(2)
        }
        writeByte(0)
    }

    override fun createDefinition() = VarClientType()
}