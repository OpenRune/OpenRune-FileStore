package dev.openrune.decoder

import dev.openrune.cache.ENUM
import dev.openrune.DefinitionDecoderOSRS
import dev.openrune.codec.EnumCodec
import dev.openrune.cache.filestore.definition.data.EnumType

class EnumDecoder : DefinitionDecoderOSRS<EnumType>(EnumCodec(), ENUM, ::EnumType)