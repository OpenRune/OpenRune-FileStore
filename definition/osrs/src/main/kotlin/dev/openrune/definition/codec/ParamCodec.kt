package dev.openrune.definition.codec

import dev.openrune.definition.util.readString
import dev.openrune.definition.util.writeString
import dev.openrune.definition.BuilderDefinitionCodec
import dev.openrune.definition.revisionIsOrAfter
import dev.openrune.definition.type.ParamType
import dev.openrune.definition.type.builders.ParamTypeBuilder
import dev.openrune.definition.util.CacheVarLiteral
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled

class ParamCodec(val rev : Int) : BuilderDefinitionCodec<ParamType, ParamTypeBuilder> {

    override fun builder(id: Int) = ParamTypeBuilder(id)

    override fun build(builder: ParamTypeBuilder) = builder.build()

    override fun ParamTypeBuilder.read(opcode: Int, buffer: ByteBuf) {
        when (opcode) {
            1 -> {
                val idx = buffer.readUnsignedByte().toInt()
                type = CacheVarLiteral.byChar(idx.toChar())
            }

            2 -> defaultInt = buffer.readInt()
            4 -> isMembers = false
            5 -> defaultString = buffer.readString()
            7 -> defaultLong = buffer.readLong()
            8 -> type = CacheVarLiteral.byID(buffer.readUnsignedByte().toInt())
        }
    }

    override fun ByteBuf.encode(definition: ParamType) {
        definition.type?.let { type ->
            val (flag, value) = if (revisionIsOrAfter(rev, 237)) 8 to type.id else 1 to type.ch.code
            writeByte(flag)
            writeByte(value)
        }
        if(definition.defaultInt != 0) {
            writeByte(2)
            writeInt(definition.defaultInt)
        }
        if(definition.isMembers) {
            writeByte(4)
        }
        if(definition.defaultString != null) {
            writeByte(5)
            writeString(definition.defaultString)
        }
        if(definition.defaultLong != 0L) {
            writeByte(7)
            writeLong(definition.defaultLong)
        }
        writeByte(0)
    }

    override fun createDefinition() = ParamType()
}
