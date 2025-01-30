package dev.openrune.decoder

import dev.openrune.cache.SEQUENCE
import dev.openrune.DefinitionDecoderOSRS
import dev.openrune.codec.SequenceCodec
import dev.openrune.cache.filestore.definition.data.SequenceType

class SequenceDecoder : DefinitionDecoderOSRS<SequenceType>(SequenceCodec(), SEQUENCE, ::SequenceType)