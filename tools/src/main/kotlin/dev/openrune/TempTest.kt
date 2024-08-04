package dev.openrune

import dev.openrune.cache.CacheManager
import dev.openrune.cache.tools.Builder
import dev.openrune.cache.tools.tasks.CacheTask
import dev.openrune.cache.tools.tasks.TaskType
import dev.openrune.cache.tools.tasks.impl.defs.PackConfig
import dev.openrune.cache.tools.tasks.impl.defs.PackMode
import java.io.File

fun main() {
    File("./cache/items").mkdirs()
    val tasks : MutableList<CacheTask> = listOf(
        PackConfig(PackMode.ITEMS,File("./cache/items")),
    ).toMutableList()

    val builder = Builder(type = TaskType.FRESH_INSTALL, 223, File("./cache/"))
    builder.extraTasks(*tasks.toTypedArray()).build().initialize()

    CacheManager.init(File("./cache/").toPath(),223)

    val item1 = CacheManager.getItem(995)
    System.out.println("Name: " + item1.name + " Server: ${item1.server}")


}