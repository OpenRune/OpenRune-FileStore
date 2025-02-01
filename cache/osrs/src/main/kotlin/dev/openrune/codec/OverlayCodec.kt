package dev.openrune.codec

import dev.openrune.buffer.Reader
import dev.openrune.buffer.Writer
import dev.openrune.cache.filestore.definition.DefinitionCodec
import dev.openrune.cache.filestore.definition.data.OverlayType

class OverlayCodec : DefinitionCodec<OverlayType> {
    override fun OverlayType.read(opcode: Int, buffer: Reader) {
        when(opcode) {
            1 -> rgbColor = buffer.readMedium()
            2 -> textureId = buffer.readUnsignedByte()
            5 -> hideUnderlay = false
            7 -> secondaryRgbColor = buffer.readMedium()
        }
    }

    override fun Writer.encode(definition: OverlayType) {
        TODO("Not yet implemented")
    }

    override fun createDefinition() = OverlayType()
}