package dev.openrune.definition.codec

import dev.openrune.definition.BuilderDefinitionCodec
import dev.openrune.definition.type.StringVectorType
import dev.openrune.definition.type.builders.StringVectorTypeBuilder
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled

class StringVectorCodec : BuilderDefinitionCodec<StringVectorType, StringVectorTypeBuilder> {

    override fun builder(id: Int) = StringVectorTypeBuilder(id)

    override fun build(builder: StringVectorTypeBuilder) = builder.build()

    override fun StringVectorTypeBuilder.read(opcode: Int, buffer: ByteBuf) {
        when(opcode) {
            2 -> persist = true
        }
    }

    override fun ByteBuf.encode(definition: StringVectorType) {
        if (definition.persist) {
            writeByte(2)
        }
        writeByte(0)
    }

    override fun createDefinition() = StringVectorType()
}
