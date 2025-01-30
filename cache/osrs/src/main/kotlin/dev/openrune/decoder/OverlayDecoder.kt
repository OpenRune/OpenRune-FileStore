package dev.openrune.decoder

import dev.openrune.cache.*
import dev.openrune.cache.filestore.definition.DefinitionDecoder
import dev.openrune.cache.filestore.buffer.Reader
import dev.openrune.cache.filestore.definition.data.*
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap

class OverlayDecoder : DefinitionDecoder<OverlayType>(CONFIGS) {

    override fun getArchive(id: Int) = OVERLAY
    override fun createDefinition(): OverlayType = OverlayType()
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