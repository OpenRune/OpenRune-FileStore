package dev.openrune.decoder

import dev.openrune.OpcodelessDecoder
import dev.openrune.cache.SPRITES
import dev.openrune.cache.filestore.definition.data.SpriteType
import dev.openrune.codec.SpriteCodec

class SpriteDecoder : OpcodelessDecoder<SpriteType>(SPRITES, { SpriteType(0) }, SpriteCodec()) {
    override fun getFile(id: Int) = 0
}