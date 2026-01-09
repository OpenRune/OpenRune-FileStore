package dev.openrune.definition.codec

import dev.openrune.definition.util.readString
import dev.openrune.definition.util.writeString
import dev.openrune.definition.DefinitionCodec
import dev.openrune.definition.type.ParamType
import dev.openrune.definition.type.VarClanSettingsType
import dev.openrune.definition.type.VarClanType
import dev.openrune.definition.util.VarType
import io.netty.buffer.ByteBuf

class VarClanSettingCodec : DefinitionCodec<VarClanSettingsType> {
    override fun VarClanSettingsType.read(opcode: Int, buffer: ByteBuf) {
        when (opcode) {
            1 -> {
                val idx = buffer.readUnsignedByte().toInt()
                type = VarType.byChar(idx.toChar())
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