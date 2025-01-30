package dev.openrune.decoder

import dev.openrune.cache.OBJECT
import dev.openrune.DefinitionDecoderOSRS
import dev.openrune.codec.ObjectCodec
import dev.openrune.cache.filestore.definition.data.ObjectType

class ObjectDecoder : DefinitionDecoderOSRS<ObjectType>(ObjectCodec(), OBJECT, ::ObjectType)