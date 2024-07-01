package dev.openrune.cache.filestore.definition.encoder

import dev.openrune.cache.filestore.buffer.Writer
import dev.openrune.cache.filestore.definition.ConfigEncoder
import dev.openrune.cache.filestore.definition.data.AreaType
import dev.openrune.cache.filestore.definition.data.HitSplatType

class AreaEncoder : ConfigEncoder<AreaType>() {

    override fun Writer.encode(definition: AreaType) {


        writeByte(0)
    }

}