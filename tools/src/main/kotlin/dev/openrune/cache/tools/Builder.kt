package dev.openrune.cache.tools

import dev.openrune.definition.RSCMHandler
import dev.openrune.cache.tools.tasks.CacheTask
import dev.openrune.cache.tools.tasks.TaskType
import java.io.File
import java.nio.file.Path

val DEFAULT_PATH = Path.of("data", "cache").toFile()

data class Builder(
    var type: TaskType,
    var revision: Int,
    var cacheLocation : File = DEFAULT_PATH,
    var extraTasks: Array<CacheTask> = emptyArray()
) {

    private var removeXteas : Boolean = true
    private var removeBzip : Boolean = true

    fun removeXteas()  = removeXteas
    fun removeBzip()  = removeBzip

    fun extraTasks(vararg types: CacheTask) = apply { this.extraTasks = types.toMutableList().toTypedArray() }

    fun removeXteas(removeXteas: Boolean) = apply { this.removeXteas = removeXteas }

    fun removeBzip(removeBzip: Boolean) = apply { this.removeBzip = removeBzip }

    fun registerRSCM(mappingsDir: File) = apply {
        RSCMHandler.load(mappingsDir)
    }

    fun cacheLocation(cacheLocation: File) = apply { this.cacheLocation = cacheLocation }

    fun build() = CacheTool(this)
}