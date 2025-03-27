package dev.openrune.cache.tools.tasks.impl

import dev.openrune.cache.MAPS
import dev.openrune.cache.util.XteaLoader
import dev.openrune.cache.tools.tasks.CacheTask
import dev.openrune.cache.util.progress
import dev.openrune.filesystem.Cache
import java.io.File

/*
 * Removes Xteas Encryption from the maps
 */
class RemoveXteas(private val xteaLocation : File) : CacheTask() {
    override fun init(cache: Cache) {
        XteaLoader.load(xteaLocation)
        var mapCount = 0
        for (x in 0..256) {
            for (y in 0..256) {
                val landscapeId = cache.archiveId(5, "l${x}_${y}")
                if (landscapeId != -1) {
                    mapCount++
                }
            }
        }
        val mapProgress = progress("Removing Xteas Maps", mapCount)

        for (x in 0..256) {
            for (y in 0..256) {
                val regionId = x shl 8 or y
                val landscapeId = cache.archiveId(5, "l${x}_${y}")
                if (landscapeId != -1) {
                    val keys = XteaLoader.getKeys(regionId)
                    val landscape = cache.data(5, "l${x}_${y}", keys)
                    if (landscape != null) {
                        cache.write(MAPS, "l${x}_${y}", landscape)
                    }
                    mapProgress.step()
                }
            }
        }
        mapProgress.close()
    }
}