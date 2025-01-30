package dev.openrune.decoder

import dev.openrune.cache.HEALTHBAR
import dev.openrune.DefinitionDecoderOSRS
import dev.openrune.codec.HealthBarCodec
import dev.openrune.cache.filestore.definition.data.HealthBarType

class HealthBarDecoder : DefinitionDecoderOSRS<HealthBarType>(HealthBarCodec(), HEALTHBAR, ::HealthBarType)