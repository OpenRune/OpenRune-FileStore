package dev.openrune.definition.codec

import dev.openrune.definition.BuilderDefinitionCodec
import dev.openrune.definition.type.VarBitType
import dev.openrune.definition.type.builders.VarBitTypeBuilder
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled

class VarBitCodec : BuilderDefinitionCodec<VarBitType, VarBitTypeBuilder> {
    override fun builder(id: Int) = VarBitTypeBuilder(id)

    override fun build(builder: VarBitTypeBuilder) = builder.build()

    override fun VarBitTypeBuilder.read(opcode: Int, buffer: ByteBuf) {
        if (opcode == 1) {
            varp = buffer.readUnsignedShort()
            startBit = buffer.readUnsignedByte().toInt()
            endBit = buffer.readUnsignedByte().toInt()
        }
    }

    override fun ByteBuf.encode(definition: VarBitType) {
        writeByte(1)
        writeShort(definition.varp)
        writeByte(definition.startBit)
        writeByte(definition.endBit)

        writeByte(0)
    }

    override fun createDefinition() = VarBitType()
}
