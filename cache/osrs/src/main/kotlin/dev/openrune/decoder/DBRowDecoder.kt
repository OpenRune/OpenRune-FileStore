package dev.openrune.decoder

import dev.openrune.cache.DBROW
import dev.openrune.DefinitionDecoderOSRS
import dev.openrune.codec.DBRowCodec
import dev.openrune.cache.filestore.definition.data.DBRowType

class DBRowDecoder : DefinitionDecoderOSRS<DBRowType>(DBRowCodec(), DBROW, ::DBRowType)