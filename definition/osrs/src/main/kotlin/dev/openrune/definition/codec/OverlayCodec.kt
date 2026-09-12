package dev.openrune.definition.codec

import dev.openrune.definition.BuilderDefinitionCodec
import dev.openrune.definition.type.OverlayType
import dev.openrune.definition.type.builders.OverlayTypeBuilder
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled

class OverlayCodec : BuilderDefinitionCodec<OverlayType, OverlayTypeBuilder> {

    override fun builder(id: Int) = OverlayTypeBuilder(id)

    override fun build(builder: OverlayTypeBuilder) = builder.build()

    override fun OverlayTypeBuilder.read(opcode: Int, buffer: ByteBuf) {
        when (opcode) {
            1 -> primaryRgb = buffer.readUnsignedMedium()
            2 -> texture = buffer.readUnsignedByte().toInt()
            5 -> hideUnderlay = false
            7 -> secondaryRgb = buffer.readUnsignedMedium()
            9 -> water = buffer.readUnsignedByte().toInt()
        }
    }

    override fun ByteBuf.encode(definition: OverlayType) {
        TODO("Not yet implemented")
    }

    override fun createDefinition() = OverlayType()
}
