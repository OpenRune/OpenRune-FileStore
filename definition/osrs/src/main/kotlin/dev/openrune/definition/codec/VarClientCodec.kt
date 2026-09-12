package dev.openrune.definition.codec

import dev.openrune.definition.BuilderDefinitionCodec
import dev.openrune.definition.type.VarClientType
import dev.openrune.definition.type.builders.VarClientTypeBuilder
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled

class VarClientCodec : BuilderDefinitionCodec<VarClientType, VarClientTypeBuilder> {

    override fun builder(id: Int) = VarClientTypeBuilder(id)

    override fun build(builder: VarClientTypeBuilder) = builder.build()

    override fun VarClientTypeBuilder.read(opcode: Int, buffer: ByteBuf) {
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
