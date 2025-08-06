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

    val cache = Cache.load(Path.of("C:\\Users\\Home\\Desktop\\New folder"))

    CacheManager.init(OsrsCacheProvider(cache,232))

    val models = ModelDecoder(cache)

    var count = 0
    cache.archives(MODELS).forEach {
        val model = models.getModel(it)
        if (model?.faceZOffsets != null) {
            println(Arrays.toString(model!!.faceZOffsets))
            count++
        }
    }
    println("ON TOTAL: $count")

//    println(CacheManager.getItem(995).toString())
//    println(CacheManager.getObject(10060).toString())
//
//    val interfacesNew = GameValHandler.readGameVal(GameValGroupTypes.VARCS, cache)
//    interfacesNew.forEach {
//        println(it.toFullString())
//    }

}