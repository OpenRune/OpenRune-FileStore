package dev.openrune.definition.codec

import dev.openrune.definition.BuilderDefinitionCodec
import dev.openrune.definition.type.VarpType
import dev.openrune.definition.type.builders.VarpTypeBuilder
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled

class VarCodec : BuilderDefinitionCodec<VarpType, VarpTypeBuilder> {
    override fun builder(id: Int) = VarpTypeBuilder(id)

    override fun build(builder: VarpTypeBuilder) = builder.build()

    override fun VarpTypeBuilder.read(opcode: Int, buffer: ByteBuf) {
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
