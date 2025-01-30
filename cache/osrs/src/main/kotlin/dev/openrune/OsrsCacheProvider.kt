package dev.openrune

import dev.openrune.cache.CLIENTSCRIPT
import dev.openrune.cache.CacheStore
import dev.openrune.cache.filestore.Cache
import dev.openrune.cache.filestore.definition.data.*
import dev.openrune.decoder.*
import io.github.oshai.kotlinlogging.KotlinLogging
import java.nio.BufferUnderflowException

class OsrsCacheProvider(private val cache : Cache, override var cacheRevision : Int = -1) : CacheStore() {

    private val logger = KotlinLogging.logger {}

    init {
        CACHE_REVISION = cacheRevision
    }

    companion object {
        var CACHE_REVISION = -1
    }

    override val npcs: MutableMap<Int, NpcType> = mutableMapOf()
    override val objects: MutableMap<Int, ObjectType> = mutableMapOf()
    override val items: MutableMap<Int, ItemType> = mutableMapOf()
    override val varbits: MutableMap<Int, VarBitType> = mutableMapOf()
    override val varps: MutableMap<Int, VarpType> = mutableMapOf()
    override val anims: MutableMap<Int, SequenceType> = mutableMapOf()
    override val enums: MutableMap<Int, EnumType> = mutableMapOf()
    override val healthBars: MutableMap<Int, HealthBarType> = mutableMapOf()
    override val hitsplats: MutableMap<Int, HitSplatType> = mutableMapOf()
    override val structs: MutableMap<Int, StructType> = mutableMapOf()

    override fun init() {
        try {
            // Use the decoders to load the definitions directly into the maps
            ObjectDecoder().load(cache, objects)
            NPCDecoder().load(cache, npcs)
            ItemDecoder().load(cache, items)
            VarBitDecoder().load(cache, varbits)
            VarDecoder().load(cache, varps)
            SequenceDecoder().load(cache, anims)
            EnumDecoder().load(cache, enums)
            HealthBarDecoder().load(cache, healthBars)
            HitSplatDecoder().load(cache, hitsplats)
            StructDecoder().load(cache, structs)
        } catch (e: BufferUnderflowException) {
            logger.error(e) { "Error reading definitions" }
            throw e
        }
    }

    fun findScriptId(name: String): Int {
        val cacheName = "[clientscript,$name]"
        return cache.archiveId(CLIENTSCRIPT, cacheName).also { id ->
            if (id == -1) println("Unable to find script: $cacheName")
        }
    }

}