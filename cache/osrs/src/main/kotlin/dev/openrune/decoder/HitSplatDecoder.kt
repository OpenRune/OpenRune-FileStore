package dev.openrune.decoder

import dev.openrune.cache.HITSPLAT
import dev.openrune.DefinitionDecoderOSRS
import dev.openrune.codec.HitSplatCodec
import dev.openrune.cache.filestore.definition.data.HitSplatType

class HitSplatDecoder : DefinitionDecoderOSRS<HitSplatType>(HitSplatCodec(), HITSPLAT, ::HitSplatType)