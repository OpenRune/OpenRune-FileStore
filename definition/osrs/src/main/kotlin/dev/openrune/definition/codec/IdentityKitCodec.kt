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
                models = readIntList(length) {
                    buffer.readInt().let { if (it == 65535) -1 else it }
                }
            }
            40 -> readColours(buffer)
            41 -> readTextures(buffer)
            in 60..70 -> chatheadModels[opcode - 60] = buffer.readUnsignedShort()
        }
    }

    override fun ByteBuf.encode(definition: IdentityKitType) {
        if(definition.bodyPartId != -1) {
            writeByte(1)
            writeByte(definition.bodyPartId)
        }

        if (revisionIsOrAfter(rev, 237)) {
            if (definition.models != null && definition.models!!.isNotEmpty()) {
                writeByte(1)
                writeByte(definition.models!!.size)
                for (i in definition.models!!.indices) {
                    writeShort(definition.models!![i])
                }
            }
        } else {
            if (definition.models != null && definition.models!!.isNotEmpty()) {
                writeByte(1)
                writeByte(definition.models!!.size)
                for (i in definition.models!!.indices) {
                    writeInt(definition.models!![i])
                }
            }
        }

        if (definition.nonSelectable) {
            writeByte(3)
        }

        definition.writeColoursTextures(this)

        if (definition.chatheadModels.any { it != -1 }) {
            for (i in 0 until definition.chatheadModels.size) {
                writeByte(60 + i)
                writeShort(definition.chatheadModels[i])
            }
        }
        writeByte(0)
    }

    override fun createDefinition() = IdentityKitType()
}
