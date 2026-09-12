package dev.openrune.definition.codec

import dev.openrune.definition.util.readString
import dev.openrune.definition.util.writeString
import dev.openrune.definition.BuilderDefinitionCodec
import dev.openrune.definition.type.VarClanSettingsType
import dev.openrune.definition.type.builders.VarClanSettingsTypeBuilder
import dev.openrune.definition.util.CacheVarLiteral
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled

class VarClanSettingCodec : BuilderDefinitionCodec<VarClanSettingsType, VarClanSettingsTypeBuilder> {

    override fun builder(id: Int) = VarClanSettingsTypeBuilder(id)

    override fun build(builder: VarClanSettingsTypeBuilder) = builder.build()

    override fun VarClanSettingsTypeBuilder.read(opcode: Int, buffer: ByteBuf) {
        when (opcode) {
            1 -> {
                val idx = buffer.readUnsignedByte().toInt()
                type = CacheVarLiteral.byChar(idx.toChar())
            }

            2 -> lifetime = buffer.readUnsignedByte().toInt()
            10 -> debugName = buffer.readString()
        }
    }

    override fun ByteBuf.encode(definition: VarClanSettingsType) {
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

    override fun createDefinition() = VarClanSettingsType()
}
