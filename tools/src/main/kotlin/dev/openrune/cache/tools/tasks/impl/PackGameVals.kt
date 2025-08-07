package dev.openrune.cache.tools.tasks.impl

import dev.openrune.cache.gameval.GameValHandler
import dev.openrune.cache.tools.CacheTool
import dev.openrune.cache.tools.tasks.CacheTask
import dev.openrune.filesystem.Cache

internal class PackGameVals() : CacheTask() {

    override fun init(cache: Cache) {
        if (revision < 230) {
            return
        }

        CacheTool.gameValMappings.forEach { (gameValGroup, values) ->
            GameValHandler.encodeGameVals(gameValGroup,values,cache,revision)
        }
    }

}
