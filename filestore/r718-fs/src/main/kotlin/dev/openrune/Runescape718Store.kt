package dev.openrune

import dev.openrune.Index.ITEMS
import dev.openrune.Index.NPCS
import dev.openrune.cache.CacheStore
import dev.openrune.filesystem.Cache
import dev.openrune.cache.filestore.definition.IndexedDefinitionDecoder
import dev.openrune.definition.type.*
import dev.openrune.definition.codec.ItemCodec718
import dev.openrune.definition.codec.NpcCodec718
import java.nio.BufferUnderflowException

class Runescape718Store(private val cache : Cache, override var cacheRevision : Int = -1) : CacheStore() {

    init {
        CACHE_REVISION = cacheRevision
    }

    companion object {
        var CACHE_REVISION = -1
    }

    override val items: MutableMap<Int, ItemType> = mutableMapOf()
    override val npcs: MutableMap<Int, NpcType> = mutableMapOf()

    override fun init() {
        try {
            ItemDecoder718().load(cache, items)
            NpcDecoder718().load(cache, npcs)
        } catch (e: BufferUnderflowException) {

        }
    }

}

class ItemDecoder718 : IndexedDefinitionDecoder<ItemType>(ITEMS, 8, ItemCodec718())
class NpcDecoder718 : IndexedDefinitionDecoder<NpcType>(NPCS, 7, NpcCodec718())