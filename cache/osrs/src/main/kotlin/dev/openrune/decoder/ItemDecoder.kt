package dev.openrune.decoder

import dev.openrune.cache.ITEM
import dev.openrune.DefinitionDecoderOSRS
import dev.openrune.codec.ItemCodec
import dev.openrune.cache.filestore.definition.data.ItemType

class ItemDecoder : DefinitionDecoderOSRS<ItemType>(ItemCodec(), ITEM, ::ItemType)