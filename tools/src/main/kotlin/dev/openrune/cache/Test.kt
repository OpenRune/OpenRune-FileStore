package dev.openrune.cache

import dev.openrune.cache.tools.Builder
import dev.openrune.cache.tools.tasks.CacheTask
import dev.openrune.cache.tools.tasks.TaskType
import dev.openrune.cache.tools.tasks.impl.defs.PackConfig
import dev.openrune.cache.tools.tasks.impl.defs.PackConfig.Companion.registerPackType
import dev.openrune.definition.codec.ObjectCodec
import java.io.File

fun main() {
    val tasks : MutableList<CacheTask> = listOf(
        PackConfig(File("C:\\Users\\Home\\Desktop\\New folder (3)"))
    ).toMutableList()

    PackConfig.packTypes.registerPackType(OBJECT, ObjectCodec::class, "object")

    val builder = Builder(type = TaskType.FRESH_INSTALL, revision = 225, File("C:\\Users\\Home\\Desktop\\New folder (2)"))
    //builder.registerRSCM(File("../mappings"))
    builder.extraTasks(*tasks.toTypedArray()).build().initialize()

}