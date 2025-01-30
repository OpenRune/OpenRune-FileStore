package dev.openrune.decoder

import dev.openrune.cache.VARPLAYER
import dev.openrune.DefinitionDecoderOSRS
import dev.openrune.codec.VarCodec
import dev.openrune.cache.filestore.definition.data.VarpType

class VarDecoder : DefinitionDecoderOSRS<VarpType>(VarCodec(), VARPLAYER, ::VarpType)