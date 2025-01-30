package dev.openrune.decoder

import dev.openrune.cache.VARBIT
import dev.openrune.DefinitionDecoderOSRS
import dev.openrune.codec.VarBitCodec
import dev.openrune.cache.filestore.definition.data.VarBitType

class VarBitDecoder : DefinitionDecoderOSRS<VarBitType>(VarBitCodec(), VARBIT, ::VarBitType)