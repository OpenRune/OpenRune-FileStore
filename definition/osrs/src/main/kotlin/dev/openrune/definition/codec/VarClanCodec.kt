package dev.openrune.definition.codec

import dev.openrune.definition.util.readString
import dev.openrune.definition.util.writeString
import dev.openrune.definition.BuilderDefinitionCodec
import dev.openrune.definition.type.VarClanType
import dev.openrune.definition.type.builders.VarClanTypeBuilder
import dev.openrune.definition.util.CacheVarLiteral
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled

class VarClanCodec : BuilderDefinitionCodec<VarClanType, VarClanTypeBuilder> {

    override fun builder(id: Int) = VarClanTypeBuilder(id)

    override fun build(builder: VarClanTypeBuilder) = builder.build()

    override fun VarClanTypeBuilder.read(opcode: Int, buffer: ByteBuf) {
        when (opcode) {
            1 -> {
                val idx = buffer.readUnsignedByte().toInt()
                type = CacheVarLiteral.byChar(idx.toChar())
            }

            2 -> lifetime = buffer.readUnsignedByte().toInt()
            10 -> debugName = buffer.readString()
        }
    }

    override fun ByteBuf.encode(definition: VarClanType) {
        if(definition.type != null) {
            writeByte(1)
            writeByte(definition.type!!.ch.code)
        }

        if(definition.lifetime != 0) {
            writeByte(2)
            writeByte(definition.lifetime)
        }

        if(definition.debugName != "") {
            writeByte(10)
            writeString(definition.debugName)
        }
        writeByte(0)
    }

    override fun createDefinition() = VarClanType()
}
