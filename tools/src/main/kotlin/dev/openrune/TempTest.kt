package dev.openrune

import dev.openrune.cache.CacheManager
import dev.openrune.cache.tools.Builder
import dev.openrune.cache.tools.tasks.CacheTask
import dev.openrune.cache.tools.tasks.TaskType
import dev.openrune.cache.tools.tasks.impl.defs.PackConfig
import dev.openrune.cache.tools.tasks.impl.defs.PackMode
import java.io.File


private var _test: UShort = 65535u

var test: Int = -1
    get() = _test.toInt()

fun main() {
    val runtime = Runtime.getRuntime()


    val CACHE = File("./cache/ ")

    val memoryBefore = runtime.totalMemory() - runtime.freeMemory()
    println("Memory before initialization: $memoryBefore bytes")

    //CacheManager.init(CACHE.toPath(), 223)
    val memoryAfter = runtime.totalMemory() - runtime.freeMemory()
    val memoryUsed = memoryAfter - memoryBefore

    println("Memory used by CacheManager: $memoryUsed bytes")


    //System.out.println("Old: " + CacheManager.getItem(1079).getModifiedColours().contentDeepToString())

    val tasks : MutableList<CacheTask> = listOf(
        PackConfig(PackMode.ITEMS,File("./items")),
    ).toMutableList()

    val builder = Builder(type = TaskType.FRESH_INSTALL, 223, CACHE)
    builder.extraTasks(*tasks.toTypedArray()).build().initialize()

    CacheManager.init(CACHE.toPath(), 223)

    System.out.println("New: " + CacheManager.getItem(1079).modifiedColours)

}