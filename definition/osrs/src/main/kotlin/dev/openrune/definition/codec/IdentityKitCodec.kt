package dev.openrune.definition.codec

import dev.openrune.definition.BuilderDefinitionCodec
import dev.openrune.definition.revisionIsOrAfter
import dev.openrune.definition.type.IdentityKitType
import dev.openrune.definition.type.builders.IdentityKitTypeBuilder
import dev.openrune.definition.util.readIntList
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled

class IdentityKitCodec(val rev : Int) : BuilderDefinitionCodec<IdentityKitType, IdentityKitTypeBuilder> {

    override fun builder(id: Int) = IdentityKitTypeBuilder(id)

    override fun build(builder: IdentityKitTypeBuilder) = builder.build()

    override fun IdentityKitTypeBuilder.read(opcode: Int, buffer: ByteBuf) {
        when (opcode) {
            1 -> bodyPartId = buffer.readUnsignedByte().toInt()
            2 -> {
                val length = buffer.readUnsignedByte().toInt()
                models = readIntList(length) {
                    buffer.readUnsignedShort().let { if (it == 65535) -1 else it }
                }
            }
            3 -> nonSelectable = true
            5 -> {
                val length = buffer.readUnsignedByte().toInt()
                models = readIntList(length) { buffer.readInt() }
            }
            40 -> readColours(buffer)
            41 -> readTextures(buffer)
            in 60..69 -> chatheadModels[opcode - 60] = buffer.readUnsignedShort().let { if (it == 65535) -1 else it }
            in 70..79 -> chatheadModels[opcode - 70] = buffer.readInt()
        }
    }

    override fun ByteBuf.encode(definition: IdentityKitType) {
        if(definition.bodyPartId != -1) {
            writeByte(1)
            writeByte(definition.bodyPartId)
        }

        val extended = revisionIsOrAfter(rev, 237)

        val models = definition.models
        if (models != null && models.isNotEmpty()) {
            writeByte(if (extended) 5 else 2)
            writeByte(models.size)
            for (model in models) {
                if (extended) writeInt(model) else writeShort(model)
            }
        }

        if (definition.nonSelectable) {
            writeByte(3)
        }

        definition.writeColoursTextures(this)

        definition.chatheadModels.forEachIndexed { index, model ->
            if (model != -1) {
                if (extended) {
                    writeByte(70 + index)
                    writeInt(model)
                } else {
                    writeByte(60 + index)
                    writeShort(model)
                }
            }
        }
        writeByte(0)
    }

    override fun createDefinition() = IdentityKitType()
}
