package dev.openrune.definition.codec

import dev.openrune.definition.type.VarClientType
import dev.openrune.definition.type.builders.VarClientTypeBuilder
import dev.openrune.definition.util.CacheVarLiteral
import io.netty.buffer.ByteBuf

class Rs2VarClientCodec : Rs2OrderedBuilderCodec<VarClientType, VarClientTypeBuilder> {

    override fun builder(id: Int) = VarClientTypeBuilder(id)

    override fun build(builder: VarClientTypeBuilder) = builder.build()

    override fun VarClientTypeBuilder.read(opcode: Int, buffer: ByteBuf) {
        when (opcode) {
            1 -> type = CacheVarLiteral.byChar(buffer.readUnsignedByte().toInt().toChar())
            2 -> persist = true
        }
    }

    override fun ByteBuf.writePayload(opcode: Int, occurrence: Int, definition: VarClientType) {
        when (opcode) {
            1 -> writeByte(definition.rs2!!.type!!.ch.code)
            2 -> Unit
        }
    }

    override fun createDefinition() = VarClientType()
}
