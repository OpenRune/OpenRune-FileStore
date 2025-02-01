package dev.openrune

import dev.openrune.cache.SPRITES
import dev.openrune.cache.filestore.definition.DefinitionDecoder
import dev.openrune.definition.codec.SpriteCodec
import dev.openrune.definition.type.SpriteType

class SpriteDecoder : DefinitionDecoder<SpriteType>(SPRITES, SpriteCodec()) {
    override fun getFile(id: Int) = 0
}