package dev.openrune.decoder

import dev.openrune.cache.AREA
import dev.openrune.DefinitionDecoderOSRS
import dev.openrune.codec.AreaCodec
import dev.openrune.cache.filestore.definition.data.AreaType

class AreaDecoder : DefinitionDecoderOSRS<AreaType>(AreaCodec(), AREA, ::AreaType)