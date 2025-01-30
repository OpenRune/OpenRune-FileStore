package dev.openrune.decoder

import dev.openrune.cache.STRUCT
import dev.openrune.DefinitionDecoderOSRS
import dev.openrune.codec.StructCodec
import dev.openrune.cache.filestore.definition.data.StructType

class StructDecoder : DefinitionDecoderOSRS<StructType>(StructCodec(), STRUCT, ::StructType)