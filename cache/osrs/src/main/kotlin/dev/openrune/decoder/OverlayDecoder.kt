package dev.openrune.decoder

import dev.openrune.cache.OVERLAY
import dev.openrune.DefinitionDecoderOSRS
import dev.openrune.codec.OverlayCodec
import dev.openrune.cache.filestore.definition.data.OverlayType

class OverlayDecoder : DefinitionDecoderOSRS<OverlayType>(OverlayCodec(), OVERLAY, ::OverlayType)