package dev.openrune.cache.filestore.definition.decoder

import dev.openrune.cache.AREA
import dev.openrune.cache.HITSPLAT
import dev.openrune.cache.filestore.definition.DefinitionDecoder
import dev.openrune.cache.filestore.buffer.Reader
import dev.openrune.cache.filestore.definition.data.AreaType
import dev.openrune.cache.filestore.definition.data.HitSplatType

class AreaDecoder : DefinitionDecoder<AreaType>(AREA) {

    override fun create(size: Int) = Array(size) { AreaType(it) }

    override fun getFile(id: Int) = id

    override fun AreaType.read(opcode: Int, buffer: Reader) {
        when (opcode) {

        }
    }
}