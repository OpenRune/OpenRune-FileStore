package dev.openrune.definition.codec

import dev.openrune.definition.DefinitionCodec
import dev.openrune.definition.type.BugTemplateType
import dev.openrune.definition.type.VarClientType
import io.netty.buffer.ByteBuf

class BugTemplateCodec : DefinitionCodec<BugTemplateType> {
    override fun BugTemplateType.read(opcode: Int, buffer: ByteBuf) {

    }

    override fun ByteBuf.encode(definition: BugTemplateType) {
        writeByte(0)
    }

    override fun createDefinition() = BugTemplateType()
}