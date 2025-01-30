package dev.openrune

import dev.openrune.cache.CacheStore
import dev.openrune.cache.filestore.Cache
import dev.openrune.cache.filestore.definition.data.*
import dev.openrune.decoder.ItemDecoder742
import java.nio.BufferUnderflowException

class Runescape742Store(private val cache : Cache, override var cacheRevision : Int = -1) : CacheStore() {

    init {
        CACHE_REVISION = cacheRevision
    }

    companion object {
        var CACHE_REVISION = -1
    }

    override val items: MutableMap<Int, ItemType> = mutableMapOf()

    override fun init() {
        try {
            ItemDecoder742().load(cache, items)
        } catch (e: BufferUnderflowException) {

        }
    }

}