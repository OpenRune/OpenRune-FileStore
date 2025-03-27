package dev.openrune.definition.codec

import dev.openrune.definition.DefinitionCodec
import dev.openrune.definition.type.VarpType
import io.netty.buffer.ByteBuf

class VarCodec : DefinitionCodec<VarpType> {
    override fun VarpType.read(opcode: Int, buffer: ByteBuf) {
        if (opcode == 5) {
            configType = buffer.readUnsignedShort()
        }
    }

    override fun ByteBuf.encode(definition: VarpType) {
        TODO("Not yet implemented")
    }

    override fun createDefinition() = VarpType()
}