package dev.openrune.decoder

import dev.openrune.OpcodelessDecoder
import dev.openrune.cache.TEXTURES
import dev.openrune.cache.filestore.definition.data.TextureType
import dev.openrune.codec.TextureCodec

class TextureDecoder : OpcodelessDecoder<TextureType>(TEXTURES, ::TextureType, TextureCodec()) {
    override fun getArchive(id: Int) = 0
}