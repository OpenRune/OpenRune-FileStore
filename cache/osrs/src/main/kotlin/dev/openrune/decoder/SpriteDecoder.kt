package dev.openrune.decoder

import dev.openrune.cache.SPRITES
import dev.openrune.cache.filestore.definition.DefinitionDecoder
import dev.openrune.cache.filestore.definition.data.SpriteType
import dev.openrune.codec.SpriteCodec

class SpriteDecoder : DefinitionDecoder<SpriteType>(SPRITES, SpriteCodec()) {
    override fun getFile(id: Int) = 0
}