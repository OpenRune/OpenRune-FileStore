package dev.openrune.cache

import dev.openrune.cache.tools.Builder
import dev.openrune.cache.tools.tasks.CacheTask
import dev.openrune.cache.tools.tasks.TaskType
import dev.openrune.cache.tools.tasks.impl.defs.PackConfig
import java.io.File
import kotlin.system.exitProcess

fun getCacheLocation() = File("./data/","cache").path
fun getRawCacheLocation(dir : String) = File("./data/","raw-cache/$dir/")

fun main(args : Array<String>) {

    val type = "update"
    val rev = 230

    val tasks : MutableList<CacheTask> = listOf(
        PackConfig(getRawCacheLocation("definitions/")),
    ).toMutableList()

    when(type) {
        "update" -> {
            val builder = Builder(type = TaskType.FRESH_INSTALL, revision = rev, File(getCacheLocation()))
            builder.removeXteas()
            builder.removeBzip()
            builder.registerRSCM(File("./mappings"))
            builder.extraTasks(*tasks.toTypedArray()).build().initialize()
        }
        "build" -> {
            val builder = Builder(type = TaskType.BUILD, revision = rev, cacheLocation = File(getCacheLocation()))
            builder.registerRSCM(File("./mappings"))
            builder.extraTasks(*tasks.toTypedArray()).build().initialize()
        }
    }
}