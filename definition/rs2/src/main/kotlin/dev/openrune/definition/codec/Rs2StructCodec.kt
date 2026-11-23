package dev.openrune.definition.codec

import dev.openrune.definition.type.StructType
import dev.openrune.definition.type.builders.StructTypeBuilder
import dev.openrune.definition.writeParameterBody
import io.netty.buffer.ByteBuf

class Rs2StructCodec : Rs2OrderedBuilderCodec<StructType, StructTypeBuilder> {

    override fun builder(id: Int) = StructTypeBuilder(id)

    override fun build(builder: StructTypeBuilder) = builder.build()

    override fun StructTypeBuilder.read(opcode: Int, buffer: ByteBuf) {
        when (opcode) {
            249 -> readParameters(buffer)
            251 -> pluginApi = false
        }
    }

    override fun ByteBuf.writePayload(opcode: Int, occurrence: Int, definition: StructType) {
        when (opcode) {
            249 -> writeParameterBody(this, definition.params)
            251 -> Unit
        }
    }

    override fun createDefinition() = StructType()
}
