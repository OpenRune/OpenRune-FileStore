package dev.openrune.definition.codec

import dev.openrune.definition.type.ParamType
import dev.openrune.definition.type.builders.ParamTypeBuilder
import dev.openrune.definition.util.CacheVarLiteral
import dev.openrune.definition.util.readSmart
import dev.openrune.definition.util.readString
import dev.openrune.definition.util.writeSmart
import dev.openrune.definition.util.writeString
import io.netty.buffer.ByteBuf

class Rs2ParamCodec : Rs2OrderedBuilderCodec<ParamType, ParamTypeBuilder> {

    override fun builder(id: Int) = ParamTypeBuilder(id)

    override fun build(builder: ParamTypeBuilder) = builder.build()

    override fun ParamTypeBuilder.read(opcode: Int, buffer: ByteBuf) {
        when (opcode) {
            1 -> type = CacheVarLiteral.byChar(buffer.readUnsignedByte().toInt().toChar())
            2 -> defaultInt = buffer.readInt()
            4 -> isMembers = false
            5 -> defaultString = buffer.readString()
            101 -> {
                type = Rs2VarLiteral.byID(buffer.readSmart())
                typeFromId = true
            }
        }
    }

    override fun ByteBuf.writePayload(opcode: Int, occurrence: Int, definition: ParamType) {
        when (opcode) {
            1 -> writeByte(definition.type!!.ch.code)
            2 -> writeInt(definition.defaultInt)
            4 -> Unit
            5 -> writeString(definition.defaultString!!)
            101 -> writeSmart(definition.type!!.id)
        }
    }

    override fun createDefinition() = ParamType()
}
