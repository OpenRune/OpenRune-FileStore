package dev.openrune.definition.codec

import dev.openrune.definition.DefinitionCodec
import dev.openrune.definition.type.StructType
import io.netty.buffer.ByteBuf

class StructCodec : DefinitionCodec<StructType> {
    override fun StructType.read(opcode: Int, buffer: ByteBuf) {
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