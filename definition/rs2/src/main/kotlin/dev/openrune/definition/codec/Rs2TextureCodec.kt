package dev.openrune.definition.codec

import dev.openrune.definition.DefinitionCodec
import dev.openrune.definition.type.Rs2TextureType
import dev.openrune.definition.util.readUnsignedShortOrNull
import io.netty.buffer.ByteBuf

class Rs2TextureCodec : DefinitionCodec<Rs2TextureType> {

    override fun Rs2TextureType.read(opcode: Int, buffer: ByteBuf) {
        averageColour = buffer.readUnsignedShort()
        opaque = buffer.readUnsignedByte().toInt() == 1
        val fileCount = buffer.readUnsignedByte().toInt()
        check(fileCount == 1) { "Unsupported RS3 texture file count: $fileCount" }
        sprite = buffer.readUnsignedShortOrNull() ?: -1
        colourAdjustment = buffer.readInt()
        animationDirection = buffer.readUnsignedByte().toInt()
        animationSpeed = buffer.readUnsignedByte().toInt()
    }

    override fun readLoop(definition: Rs2TextureType, buffer: ByteBuf) {
        definition.read(-1, buffer)
    }

    override fun ByteBuf.encode(definition: Rs2TextureType) {
        writeShort(definition.averageColour)
        writeByte(if (definition.opaque) 1 else 0)
        writeByte(1)
        writeShort(definition.sprite)
        writeInt(definition.colourAdjustment)
        writeByte(definition.animationDirection)
        writeByte(definition.animationSpeed)
    }

    override fun createDefinition() = Rs2TextureType()
}
