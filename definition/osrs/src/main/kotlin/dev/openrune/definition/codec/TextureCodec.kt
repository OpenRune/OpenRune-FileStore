package dev.openrune.definition.codec

import dev.openrune.definition.BuilderDefinitionCodec
import dev.openrune.definition.revisionIsOrBefore
import dev.openrune.definition.type.DEFAULT_TEXTURE_SIZE
import dev.openrune.definition.type.TextureType
import dev.openrune.definition.type.builders.TextureTypeBuilder
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled

class TextureCodec(private val revision: Int) : BuilderDefinitionCodec<TextureType, TextureTypeBuilder> {

    override fun builder(id: Int) = TextureTypeBuilder(id)

    override fun build(builder: TextureTypeBuilder) = builder.build()

    override fun TextureTypeBuilder.read(opcode: Int, buffer: ByteBuf) {
        if (revisionIsOrBefore(revision,232)) {
            averageRgb = buffer.readUnsignedShort()
            isTransparent = buffer.readUnsignedByte().toInt() == 1
            isLowDetail = DEFAULT_TEXTURE_SIZE == 64
            val count: Int = buffer.readUnsignedByte().toInt()
            if (count in 1..4) {
                fileId = buffer.readUnsignedShort()
                //This is only ever 1 in osrs and combine, and colour is never used
                if (count > 1) {
                    val combineModes = IntArray(count - 1).toMutableList()
                    for (index in 0 until count - 1) {
                        combineModes[index] = buffer.readUnsignedShort()
                    }

                    val field2440 = IntArray(count - 1).toMutableList()
                    for (index in 0 until count - 1) {
                        field2440[index] = buffer.readUnsignedShort()
                    }

                }

                val colourAdjustments = IntArray(count).toMutableList()
                for (index in 0 until count) {
                    colourAdjustments[index] = buffer.readInt()
                }

                animationDirection = buffer.readUnsignedByte().toInt()
                animationSpeed = buffer.readUnsignedByte().toInt()
            }
        } else {
            fileId = buffer.readUnsignedShort()
            averageRgb = buffer.readUnsignedShort()
            // Transparency, as in the pre-233 format; detail level is never a cache field.
            isTransparent = buffer.readUnsignedByte().toInt() == 1
            isLowDetail = DEFAULT_TEXTURE_SIZE == 64
            animationDirection = buffer.readUnsignedByte().toInt()
            animationSpeed = buffer.readUnsignedByte().toInt()
        }
    }

    override fun ByteBuf.encode(definition: TextureType) {
        if (revisionIsOrBefore(revision,232)) {
            writeShort(definition.averageRgb)
            writeByte(if(definition.isTransparent) 1 else 0)
            val fileCount = 1
            writeByte(fileCount)
            writeShort(definition.fileId)
            writeByte(definition.animationDirection)
            writeByte(definition.animationSpeed)
        } else {
            writeShort(definition.fileId)
            writeShort(definition.averageRgb)
            writeByte(if (definition.isTransparent) 1 else 0)
            writeByte(definition.animationDirection)
            writeByte(definition.animationSpeed)
        }
    }

    override fun createDefinition() = TextureType()
}
