package dev.openrune.definition.codec

import dev.openrune.definition.BuilderDefinitionCodec
import dev.openrune.definition.type.BugTemplateType
import dev.openrune.definition.type.builders.BugTemplateTypeBuilder
import io.netty.buffer.ByteBuf

class BugTemplateCodec : BuilderDefinitionCodec<BugTemplateType, BugTemplateTypeBuilder> {

    override fun builder(id: Int) = BugTemplateTypeBuilder(id)

    override fun build(builder: BugTemplateTypeBuilder) = builder.build()

    @Suppress("UNUSED_PARAMETER")
    override fun BugTemplateTypeBuilder.read(opcode: Int, buffer: ByteBuf) {
    }

    override fun ByteBuf.encode(definition: BugTemplateType) {
        writeByte(0)
    }

    override fun createDefinition() = BugTemplateType()
}
