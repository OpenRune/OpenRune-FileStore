package dev.openrune.definition.codec

import dev.openrune.definition.BuilderDefinitionCodec
import dev.openrune.definition.type.UnderlayType
import dev.openrune.definition.type.builders.UnderlayTypeBuilder
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled

class UnderlayCodec : BuilderDefinitionCodec<UnderlayType, UnderlayTypeBuilder> {

    override fun builder(id: Int) = UnderlayTypeBuilder(id)

    override fun build(builder: UnderlayTypeBuilder) = builder.build()

    override fun UnderlayTypeBuilder.read(opcode: Int, buffer: ByteBuf) {
        if (opcode == 1) {
            rgb = buffer.readUnsignedMedium()
        }
        applyHsl = true
    }

    override fun ByteBuf.encode(definition: UnderlayType) {
        TODO("Not yet implemented")
    }

    override fun createDefinition() = UnderlayType()
}
