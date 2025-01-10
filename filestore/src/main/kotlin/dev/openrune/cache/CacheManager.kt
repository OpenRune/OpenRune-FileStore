package dev.openrune.cache

import dev.openrune.cache.filestore.Cache
import dev.openrune.cache.filestore.GameDataSource
import dev.openrune.cache.filestore.definition.Definition
import dev.openrune.cache.filestore.definition.data.*
import java.nio.file.Path

object CacheManager {

    var cacheRevision = -1

    private val combinedNpcs: MutableMap<Int, NpcType> = mutableMapOf()

    @JvmStatic
    fun init(vararg dataSources : GameDataSource) {
        for (data in dataSources) {
            data.init()
            combinedNpcs.putAll(applyIdOffset(data.npcs, data.npcOffset))
        }
    }

    private fun <T : Definition> applyIdOffset(definitions: MutableMap<Int, T>, offset: Int): MutableMap<Int, T> {
        return if (offset != 0) {
            definitions.mapKeys { (key, definition) ->
                val newKey = key + offset
                definition.id = newKey
                newKey
            }.toMutableMap()
        } else {
            definitions.toMutableMap()
        }
    }

    private inline fun <reified T> getFromCombinedMap(
        map: Map<Int, T>,
        id: Int,
        typeName: String
    ): T? {
        val result = map.get(id)
        return result
    }

    fun getNpc(id: Int): NpcType? {
        return getFromCombinedMap(combinedNpcs, id, "Npc")
    }

    // Cache revision methods
    fun revisionIsOrAfter(rev: Int) = rev <= cacheRevision
    fun revisionIsOrBefore(rev: Int) = rev >= cacheRevision
    
}
