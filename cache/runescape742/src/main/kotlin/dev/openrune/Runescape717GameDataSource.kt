package dev.openrune

import dev.openrune.cache.GameDataSource
import dev.openrune.cache.filestore.Cache
import dev.openrune.cache.filestore.definition.data.ItemType
import dev.openrune.runescape742.decoders.ItemDecoder


class Runescape717GameDataSource(val cache : Cache, val cacheRevision : Int) : GameDataSource() {


    override val items: MutableMap<Int, ItemType> = mutableMapOf()

    override fun init() {
        items.putAll(ItemDecoder().load(cache,true))
    }

}