package dev.openrune.definition.codec

import dev.openrune.definition.BuilderDefinitionCodec
import dev.openrune.definition.type.StructType
import dev.openrune.definition.type.builders.StructTypeBuilder
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled

class StructCodec : BuilderDefinitionCodec<StructType, StructTypeBuilder> {
    override fun builder(id: Int) = StructTypeBuilder(id)

    override fun build(builder: StructTypeBuilder) = builder.build()

    override fun StructTypeBuilder.read(opcode: Int, buffer: ByteBuf) {
        if (opcode == 249) {
            readParameters(buffer)
        }
    }

    override fun ByteBuf.encode(definition: StructType) {
        definition.writeParameters(this)

        writeByte(0)
    }

    override fun createDefinition() = StructType()
}
