package dev.openrune.cache

import dev.openrune.OsrsCacheProvider
import dev.openrune.cache.tools.Builder
import dev.openrune.cache.tools.tasks.CacheTask
import dev.openrune.cache.tools.tasks.TaskType
import dev.openrune.cache.tools.tasks.impl.defs.PackConfig
import dev.openrune.filesystem.Cache
import java.io.File
import java.nio.file.Path


fun main() {
    val tasks : MutableList<CacheTask> = listOf(
        PackConfig(File("E:\\RSPS\\Illerai\\Cadarn-Server\\data\\raw-cache\\definitions\\enum"))
    ).toMutableList()

    val builder = Builder(type = TaskType.FRESH_INSTALL, revision = 230, File("E:\\RSPS\\Illerai\\Cadarn-Server\\data\\cache"))
    builder.registerRSCM(File("E:\\RSPS\\Illerai\\Cadarn-Server\\mappings"))
    builder.extraTasks(*tasks.toTypedArray()).build().initialize()

    CacheManager.init(OsrsCacheProvider(Cache.load(Path.of("E:\\RSPS\\Illerai\\Cadarn-Server\\data\\cache"),false),230))

    CacheManager.getEnum(65526)!!.values.forEach {
        println(it)
    }

}