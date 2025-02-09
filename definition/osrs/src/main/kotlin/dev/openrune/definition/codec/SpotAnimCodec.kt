package dev.openrune.definition.codec

import io.netty.buffer.ByteBuf
import dev.openrune.buffer.Writer
import dev.openrune.buffer.readUnsignedByteRD
import dev.openrune.buffer.readUnsignedShortRD
import dev.openrune.definition.DefinitionCodec
import dev.openrune.definition.type.SpotAnimType

class SpotAnimCodec : DefinitionCodec<SpotAnimType> {
    override fun SpotAnimType.read(opcode: Int, buffer: ByteBuf) {
        when(opcode) {
            1 -> modelId = buffer.readUnsignedShortRD()
            2 -> animationId = buffer.readUnsignedShortRD()
            4 -> resizeX = buffer.readUnsignedShortRD()
            5 -> resizeY = buffer.readUnsignedShortRD()
            6 -> rotation = buffer.readUnsignedShortRD()
            7 -> ambient = buffer.readUnsignedByteRD()
            8 -> contrast = buffer.readUnsignedByteRD()
            40 -> readColours(buffer)
            41 -> readTextures(buffer)
        }
    }

    override fun Writer.encode(definition: SpotAnimType) {
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