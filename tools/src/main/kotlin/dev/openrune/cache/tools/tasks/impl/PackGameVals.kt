package dev.openrune.cache.tools.tasks.impl

import dev.openrune.cache.GAMEVALS
import dev.openrune.cache.tools.CacheTool
import dev.openrune.cache.tools.tasks.CacheTask
import dev.openrune.definition.Js5GameValGroup
import dev.openrune.filesystem.Cache

internal class PackGameVals(private val rev : Int) : CacheTask() {

    override fun init(cache: Cache) {
        if (rev < 230) {
            return
        }

        CacheTool.gameValMappings.forEach { (group, values) ->
            val lastFileId = cache.lastFileId(GAMEVALS, group.id)
            packGameVal(group, lastFileId, values, cache)
        }
    }

    private fun packGameVal(type: Js5GameValGroup, lastFileId: Int, values: List<Pair<String, Int>>, cache: Cache) {
        when (type) {
            Js5GameValGroup.TABLETYPES, Js5GameValGroup.IFTYPES -> error("Not Supported Yet")

            else -> {
                val usedIds = values.map { it.second }.toSet()
                val maxUsedId = usedIds.max()
                val missingIds = ((lastFileId + 1)..maxUsedId).filterNot { it in usedIds }

                missingIds.forEach { cache.write(GAMEVALS, type.id, it, byteArrayOf()) }

                for ((name, id) in values) {
                    val data = formatString(name).encodeToByteArray()
                    cache.write(GAMEVALS, type.id, id, data)
                }
            }
        }
    }

    private fun formatString(input: String): String {
        return input.lowercase().replace(" ", "_")
    }
}
