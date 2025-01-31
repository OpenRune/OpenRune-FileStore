package dev.openrune.decoder

import dev.openrune.cache.TEXTURES
import dev.openrune.cache.filestore.definition.DefinitionDecoder
import dev.openrune.cache.filestore.definition.data.TextureType
import dev.openrune.codec.TextureCodec

class TextureDecoder : DefinitionDecoder<TextureType>(TEXTURES, TextureCodec()) {
    override fun getArchive(id: Int) = 0
    override fun getFile(id: Int) = 0
}