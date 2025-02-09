package dev.openrune.definition.codec

import dev.openrune.definition.DefinitionCodec
import dev.openrune.definition.type.UnderlayType
import io.netty.buffer.ByteBuf

class UnderlayCodec : DefinitionCodec<UnderlayType> {
    override fun UnderlayType.read(opcode: Int, buffer: ByteBuf) {
        if (opcode == 1) {
            color = buffer.readMedium()
        }
    }

    override fun ByteBuf.encode(definition: UnderlayType) {
        TODO("Not yet implemented")
    }

    override fun createDefinition() = UnderlayType()
}