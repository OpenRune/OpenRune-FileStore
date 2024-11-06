package dev.openrune.cache

import dev.openrune.cache.filestore.Cache
import dev.openrune.cache.filestore.definition.data.*
import dev.openrune.cache.filestore.definition.decoder.*
import java.nio.file.Path

object CacheManager {

    lateinit var cache: Cache
    var cacheRevision = -1

    private val npcs = mutableMapOf<Int, NpcType>()

    fun init(cachePath: Path, cacheRevision: Int) {
        init(Cache.load(cachePath, false), cacheRevision)
    }

    @JvmStatic
    fun init(cache: Cache, cacheRevision: Int) {
        this.cacheRevision = cacheRevision
        this.cache = cache
        npcs.putAll(NPCDecoder().load(cache))
    }

    private inline fun <T> getOrDefault(map: Map<Int, T>, id: Int, default: T, typeName: String): T {
        return map.getOrDefault(id, default).also {
            if (id == -1) println("$typeName with id $id is missing.")
        }
    }

    fun getNpc(id: Int) = npcs[id]


    fun getNpcOrDefault(id: Int) = getOrDefault(npcs, id, NpcType(), "Npc")

    fun findScriptId(name: String): Int {
        val cacheName = "[clientscript,$name]"
        return cache.archiveId(CLIENTSCRIPT, cacheName).also { id ->
            if (id == -1) println("Unable to find script: $cacheName")
        }
    }

    // Size methods
    fun npcSize() = npcs.size

    // Cache revision methods
    fun revisionIsOrAfter(rev: Int) = rev <= cacheRevision
    fun revisionIsOrBefore(rev: Int) = rev >= cacheRevision
    
}
