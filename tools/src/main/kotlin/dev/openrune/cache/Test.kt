package dev.openrune.cache

import dev.openrune.OsrsCacheProvider
import dev.openrune.cache.filestore.definition.ModelDecoder
import dev.openrune.cache.gameval.GameValHandler
import dev.openrune.cache.gameval.GameValHandler.elementAs
import dev.openrune.cache.gameval.impl.Interface
import dev.openrune.cache.tools.Builder
import dev.openrune.cache.tools.tasks.CacheTask
import dev.openrune.cache.tools.tasks.TaskType
import dev.openrune.cache.tools.tasks.impl.defs.PackConfig
import dev.openrune.definition.GameValGroupTypes
import dev.openrune.definition.GameValGroupTypes.TABLETYPES
import dev.openrune.filesystem.Cache
import java.io.File
import java.nio.file.Path
import java.util.Arrays


fun main() {

    //val builder = Builder(type = TaskType.FRESH_INSTALL, revision = 232, File("C:\\Users\\Home\\Desktop\\New folder"))
    //builder.extraTasks().build().initialize()

    val cache = Cache.load(Path.of("C:\\Users\\chris\\Desktop\\cache-oldschool-live-en-b233-2025-09-10-10-45-05-openrs2#2293\\cache"))

    CacheManager.init(OsrsCacheProvider(cache,233))

    println(CacheManager.getNpc(3029))

}