package dev.openrune.cache.filestore.definition.decoder

import dev.openrune.cache.CONFIGS
import dev.openrune.cache.DBROW
import dev.openrune.cache.UNDERLAY
import dev.openrune.cache.VARPLAYER
import dev.openrune.cache.filestore.definition.DefinitionDecoder
import dev.openrune.cache.filestore.buffer.Reader
import dev.openrune.cache.filestore.definition.data.UnderlayType
import dev.openrune.cache.filestore.definition.data.VarpType

class UnderlayDecoder : DefinitionDecoder<UnderlayType>(CONFIGS) {

    override fun getArchive(id: Int) = UNDERLAY

    override fun create(size: Int) = Array(size) { UnderlayType(it) }

    override fun getFile(id: Int) = id

    override fun UnderlayType.read(opcode: Int, buffer: Reader) {
        if (opcode == 1) {
            color = buffer.readMedium()
        }
    }
}