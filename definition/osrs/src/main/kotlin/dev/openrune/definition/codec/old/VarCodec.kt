package dev.openrune.definition.codec.old

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
        writeByte(5)
        writeShort(definition.configType)

        writeByte(0)
    }

    override fun createDefinition() = VarpType()
}