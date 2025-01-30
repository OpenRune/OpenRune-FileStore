package dev.openrune.decoder

import dev.openrune.cache.PARAMS
import dev.openrune.DefinitionDecoderOSRS
import dev.openrune.codec.ParamCodec
import dev.openrune.cache.filestore.definition.data.ParamType

class ParamDecoder : DefinitionDecoderOSRS<ParamType>(ParamCodec(), PARAMS, ::ParamType)