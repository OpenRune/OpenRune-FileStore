package dev.openrune.cache.tools

import dev.openrune.definition.RSCMHandler
import dev.openrune.cache.tools.tasks.CacheTask
import dev.openrune.cache.tools.tasks.TaskType
import dev.openrune.cache.tools.tasks.impl.PackGameVals
import dev.openrune.cache.tools.tasks.impl.RemoveBzip
import dev.openrune.cache.tools.tasks.impl.RemoveXteas
import java.io.File
import java.nio.file.Path

val DEFAULT_PATH = Path.of("data", "cache").toFile()

@Deprecated(
    message = "Builder is deprecated in kotlin. Use the CacheToolDsl class instead.",
    replaceWith = ReplaceWith("CacheToolDsl", "dev.openrune.cache.tools.CacheToolDsl")
)
data class Builder(
    var type: TaskType,
    var cacheLocation : File = DEFAULT_PATH,
    var extraTasks: Array<CacheTask> = emptyArray()
) {

    private var revision: Int = -1
    private var removeXteas : Boolean = true
    private var removeBzip : Boolean = true

    fun removeXteas()  = removeXteas
    fun removeBzip()  = removeBzip

    fun extraTasks(vararg types: CacheTask) = apply { this.extraTasks = types.toMutableList().toTypedArray() }

    fun revision(revision: Int) = apply { this.revision = revision }

    fun removeXteas(removeXteas: Boolean) = apply { this.removeXteas = removeXteas }

    fun removeBzip(removeBzip: Boolean) = apply { this.removeBzip = removeBzip }

    fun registerRSCM(mappingsDir: File) = apply {
        RSCMHandler.load(mappingsDir)
    }

    fun cacheLocation(cacheLocation: File) = apply { this.cacheLocation = cacheLocation }

    fun build() : CacheTool {

        val tasks = extraTasks.toMutableList()
        if (type == TaskType.FRESH_INSTALL) {
            if (removeBzip) tasks.add(0, RemoveBzip())
            if (removeXteas) tasks.add(1, RemoveXteas(File(cacheLocation, "xteas.json")))
        }

        tasks.add(PackGameVals())

        return CacheTool(type,revision,cacheLocation, tasks)
    }
}