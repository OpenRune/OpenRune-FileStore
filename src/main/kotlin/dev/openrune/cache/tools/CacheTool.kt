package dev.openrune.cache.tools

import com.displee.cache.CacheLibrary
import dev.openrune.cache.DownloadOSRS
import dev.openrune.cache.filestore.Cache
import dev.openrune.cache.tools.tasks.CacheTask
import dev.openrune.cache.tools.tasks.TaskType
import dev.openrune.cache.tools.tasks.impl.PackMaps
import dev.openrune.cache.tools.tasks.impl.PackModels
import dev.openrune.cache.tools.tasks.impl.RemoveXteas
import dev.openrune.cache.tools.tasks.impl.defs.PackItems
import dev.openrune.cache.util.FileUtil
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.File
import kotlin.system.exitProcess

class CacheTool(configs: Builder) {

    private val logger = KotlinLogging.logger {}

    init {
        builder = configs
    }

    companion object {
        var library : CacheLibrary? = null
        lateinit var builder : Builder
    }

    fun initialize() {

        println(builder.toString())

        if (builder.cacheLocation == DEFAULT_PATH) logger.info { "Using Default path of [${DEFAULT_PATH.absolutePath}]" }

        when(builder.type) {
            TaskType.RUN_JS5 -> println("sfsd")
            TaskType.BUILD -> buildCache()
            TaskType.FRESH_INSTALL -> {
                DownloadOSRS.init()
                buildCache()
            }
        }
    }

    private fun buildCache() {
        val tempPath = FileUtil.getTemp()
        builder.cacheLocation.listFiles { file -> file.extension.contains("dat") || file.extension.contains("idx") }
            ?.forEach { file ->
                val tempFile = File(tempPath, file.name)
                file.copyTo(tempFile, true)
            }

        library = CacheLibrary((if(builder.type == TaskType.BUILD) FileUtil.getTemp() else FileUtil.getTempDir("cache")).toString())

        try {
            runPacking()
        } catch (e: Exception) {
            tempPath.delete()
            e.printStackTrace()
            logger.error { "Unable to build cache" }
            exitProcess(0)
        }
    }

    fun runPacking() {

        library?.let {
            builder.extraTasks.forEach { task ->
                task.init(it)
            }

            it.update()
            it.rebuild(FileUtil.getTempDir("rebuilt"))
            it.close()

            val tempPath = builder.cacheLocation

            FileUtil.getTempDir("rebuilt").listFiles()?.filter { it.extension.contains("dat") || it.extension.contains("idx") }
                ?.forEach { file ->
                    val loc = File(tempPath,file.name)
                    file.copyTo(loc,true)
                }

            FileUtil.getTemp().deleteRecursively()
        } ?: run {
            error("Unable to load the cache")
        }

    }

}

fun main() {

    val tasks : Array<CacheTask> = arrayOf(
        PackItems(File("./custom/definitions/items/")),
        PackMaps(File("./custom/maps/"),File("./data/cache/xteas.json"),)
    )


    val builder2 = Builder(type = TaskType.BUILD, revision = 220)
    builder2.extraTasks(*tasks).build().initialize()

}