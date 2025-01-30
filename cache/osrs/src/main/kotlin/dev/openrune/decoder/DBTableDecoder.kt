package dev.openrune.decoder

import dev.openrune.cache.DBTABLE
import dev.openrune.DefinitionDecoderOSRS
import dev.openrune.codec.DBTableCodec
import dev.openrune.cache.filestore.definition.data.DBTableType

class DBTableDecoder : DefinitionDecoderOSRS<DBTableType>(DBTableCodec(), DBTABLE, ::DBTableType)