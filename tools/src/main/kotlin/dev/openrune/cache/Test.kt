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


fun main() {

    //val builder = Builder(type = TaskType.FRESH_INSTALL, revision = 232, File("C:\\Users\\Home\\Desktop\\New folder"))
    //builder.extraTasks().build().initialize()

    val cache = Cache.load(Path.of("C:\\Users\\Home\\Desktop\\New folder"))

    CacheManager.init(OsrsCacheProvider(cache,232))

    val models = ModelDecoder(cache)

    cache.archives(MODELS).forEach {
        println(models.getModel(it))
    }

//    println(CacheManager.getItem(995).toString())
//    println(CacheManager.getObject(10060).toString())
//
//    val interfacesNew = GameValHandler.readGameVal(GameValGroupTypes.VARCS, cache)
//    interfacesNew.forEach {
//        println(it.toFullString())
//    }

}