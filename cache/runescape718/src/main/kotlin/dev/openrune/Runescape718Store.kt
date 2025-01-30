package dev.openrune

import dev.openrune.cache.CacheStore
import dev.openrune.cache.filestore.Cache
import dev.openrune.cache.filestore.definition.data.*
import dev.openrune.decoder.ItemDecoder718
import dev.openrune.decoder.NpcDecoder718
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