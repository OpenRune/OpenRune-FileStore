package dev.openrune.definition.codec

import io.netty.buffer.ByteBuf
import dev.openrune.buffer.Writer
import dev.openrune.buffer.readMediumRD
import dev.openrune.definition.DefinitionCodec
import dev.openrune.definition.type.UnderlayType

class UnderlayCodec : DefinitionCodec<UnderlayType> {
    override fun UnderlayType.read(opcode: Int, buffer: ByteBuf) {
        if (opcode == 1) {
            color = buffer.readMediumRD()
        }
    }

    override fun Writer.encode(definition: UnderlayType) {
        TODO("Not yet implemented")
    }

    override fun createDefinition() = UnderlayType()
}