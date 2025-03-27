package dev.openrune.definition.codec

import dev.openrune.definition.DefinitionCodec
import dev.openrune.definition.type.SpotAnimType
import io.netty.buffer.ByteBuf

class SpotAnimCodec : DefinitionCodec<SpotAnimType> {
    override fun SpotAnimType.read(opcode: Int, buffer: ByteBuf) {
        when (opcode) {
            1 -> modelId = buffer.readUnsignedShort()
            2 -> animationId = buffer.readUnsignedShort()
            4 -> resizeX = buffer.readUnsignedShort()
            5 -> resizeY = buffer.readUnsignedShort()
            6 -> rotation = buffer.readUnsignedShort()
            7 -> ambient = buffer.readUnsignedByte().toInt()
            8 -> contrast = buffer.readUnsignedByte().toInt()
            40 -> readColours(buffer)
            41 -> readTextures(buffer)
        }
    }

    override fun ByteBuf.encode(definition: SpotAnimType) {
        if (definition.modelId != 0) {
            writeByte(1)
            writeShort(definition.modelId)
        }
        if (definition.animationId != -1) {
            writeByte(2)
            writeShort(definition.animationId)
        }
        if (definition.resizeX != 128) {
            writeByte(4)
            writeShort(definition.resizeX)
        }
        if (definition.resizeY != 128) {
            writeByte(5)
            writeShort(definition.resizeY)
        }
        if (definition.rotation != 0) {
            writeByte(6)
            writeShort(definition.rotation)
        }
        if (definition.ambient != 0) {
            writeByte(7)
            writeByte(definition.ambient)
        }
        if (definition.contrast != 0) {
            writeByte(8)
            writeByte(definition.contrast)
        }
        definition.writeColoursTextures(this)

        writeByte(0)
    }

    override fun createDefinition() = SpotAnimType()
}