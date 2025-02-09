package dev.openrune.definition.codec

import io.netty.buffer.ByteBuf
import dev.openrune.buffer.Writer
import dev.openrune.buffer.readMediumRD
import dev.openrune.buffer.readUnsignedByteRD
import dev.openrune.definition.DefinitionCodec
import dev.openrune.definition.type.OverlayType

class OverlayCodec : DefinitionCodec<OverlayType> {
    override fun OverlayType.read(opcode: Int, buffer: ByteBuf) {
        when(opcode) {
            1 -> rgbColor = buffer.readMediumRD()
            2 -> textureId = buffer.readUnsignedByteRD()
            5 -> hideUnderlay = false
            7 -> secondaryRgbColor = buffer.readMediumRD()
        }
    }

    override fun Writer.encode(definition: OverlayType) {
        TODO("Not yet implemented")
    }

    override fun createDefinition() = OverlayType()
}