package dev.openrune.definition.codec

import io.netty.buffer.ByteBuf
import dev.openrune.buffer.Writer
import dev.openrune.buffer.readUnsignedShortRD
import dev.openrune.definition.DefinitionCodec
import dev.openrune.definition.type.VarpType

class VarCodec : DefinitionCodec<VarpType> {
    override fun VarpType.read(opcode: Int, buffer: ByteBuf) {
        if (opcode == 5) {
            configType = buffer.readUnsignedShortRD()
        }
    }

    override fun Writer.encode(definition: VarpType) {
        TODO("Not yet implemented")
    }

    override fun createDefinition() = VarpType()
}