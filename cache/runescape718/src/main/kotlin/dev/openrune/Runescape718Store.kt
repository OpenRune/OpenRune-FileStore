package dev.openrune

import dev.openrune.Index.ENUMS
import dev.openrune.Index.ITEMS
import dev.openrune.Index.NPCS
import dev.openrune.Index.VAR_BIT
import dev.openrune.cache.CacheStore
import dev.openrune.cache.filestore.Cache
import dev.openrune.cache.filestore.definition.IndexedDefinitionDecoder
import dev.openrune.cache.filestore.definition.data.*
import dev.openrune.codec.EnumCodec
import dev.openrune.codec.ItemCodec718
import dev.openrune.codec.NpcCodec718
import dev.openrune.codec.VarBitCodec
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
    override val varbits: MutableMap<Int, VarBitType> = mutableMapOf()
    override val enums: MutableMap<Int, EnumType> = mutableMapOf()

    override fun init() {
        try {
            ItemDecoder718().load(cache, items)
            NpcDecoder718().load(cache, npcs)
            VarBitDecoder718().load(cache, varbits)
            EnumDecoder718().load(cache, enums)
        } catch (e: BufferUnderflowException) {

        }
    }

}

class ItemDecoder718 : IndexedDefinitionDecoder<ItemType>(ITEMS, 8, ItemCodec718())
class NpcDecoder718 : IndexedDefinitionDecoder<NpcType>(NPCS, 7, NpcCodec718())
class EnumDecoder718 : IndexedDefinitionDecoder<EnumType>(ENUMS, 8,EnumCodec())
class VarBitDecoder718 : IndexedDefinitionDecoder<VarBitType>(VAR_BIT, 10,VarBitCodec()) {
    override fun size(cache: Cache): Int {
        return cache.lastArchiveId(index) * 0x400 + cache.fileCount(index, cache.lastArchiveId(index))
    }
}