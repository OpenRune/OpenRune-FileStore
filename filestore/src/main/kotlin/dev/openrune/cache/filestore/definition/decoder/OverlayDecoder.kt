package dev.openrune.cache.filestore.definition.decoder

import dev.openrune.cache.*
import dev.openrune.cache.filestore.definition.DefinitionDecoder
import dev.openrune.cache.filestore.buffer.Reader
import dev.openrune.cache.filestore.definition.data.OverlayType
import dev.openrune.cache.filestore.definition.data.UnderlayType
import dev.openrune.cache.filestore.definition.data.VarpType

class OverlayDecoder : DefinitionDecoder<OverlayType>(CONFIGS) {

    override fun getArchive(id: Int) = OVERLAY

    override fun create(size: Int) = Array(size) { OverlayType(it) }

    override fun getFile(id: Int) = id

    override fun OverlayType.read(opcode: Int, buffer: Reader) {
        when(opcode) {
            1 -> rgbColor = buffer.readMedium()
            2 -> textureId = buffer.readUnsignedByte()
            5 -> hideUnderlay = false
            7 -> secondaryRgbColor = buffer.readMedium()
        }
    }
}