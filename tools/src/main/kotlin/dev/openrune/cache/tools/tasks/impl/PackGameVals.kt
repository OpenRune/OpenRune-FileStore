package dev.openrune.cache.tools.tasks.impl

import dev.openrune.cache.gameval.GameValHandler
import dev.openrune.cache.tools.CacheTool
import dev.openrune.cache.tools.TaskPriority
import dev.openrune.cache.tools.tasks.CacheTask
import dev.openrune.filesystem.Cache

/**
 * Encodes every gameval collected during the build into the cache. Runs at
 * [TaskPriority.GAMEVALS_ENCODE]: after every packer that registers gamevals, and before the CS2
 * stage, whose symbol dump reads the gamevals back out of the cache.
 */
internal class PackGameVals() : CacheTask() {

    override val priority: TaskPriority
        get() = TaskPriority.GAMEVALS_ENCODE

    override fun init(cache: Cache) {
        if (revision < 230) {
            return
        }

        CacheTool.gameValMappings.forEach { (gameValGroup, values) ->
            GameValHandler.encodeGameVals(gameValGroup,values,cache,revision)
        }
    }

}
