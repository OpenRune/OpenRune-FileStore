package dev.openrune.decoder

import dev.openrune.cache.UNDERLAY
import dev.openrune.DefinitionDecoderOSRS
import dev.openrune.codec.UnderlayCodec
import dev.openrune.cache.filestore.definition.data.UnderlayType

class UnderlayDecoder : DefinitionDecoderOSRS<UnderlayType>(UnderlayCodec(), UNDERLAY, ::UnderlayType)