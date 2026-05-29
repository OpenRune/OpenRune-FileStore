package dev.openrune.cache.tools


import com.displee.cache.CacheLibrary
import dev.openrune.cache.CacheDelegate
import dev.openrune.cache.tools.tasks.TaskType
import dev.openrune.cache.tools.tasks.impl.PackMaps
import dev.openrune.filesystem.Cache
import java.io.File
import kotlin.io.path.Path

fun main() {
//
//    val builder = Builder(
//        type = TaskType.BUILD,
//        cacheLocation = File("C:\\Users\\chris\\Desktop\\Images")
//    )
//    builder.extraTasks(PackMaps(File("D:\\RSPS\\Fluxious\\Flux-Server\\.data\\raw-cache\\map")))
//    builder.revision(238)
//    builder.build().initialize()

    val cache = CacheDelegate(CacheLibrary("D:\\RSPS\\Fluxious\\Flux-Server\\.data\\cache\\LIVE"))

    val mapPacker = WorldMapPacker(cache)
    mapPacker.pack(Path("D:\\RSPS\\Fluxious\\Flux-Server\\.data\\cache\\LIVE"))
}