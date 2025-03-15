package dev.openrune.cache.tools

import dev.openrune.definition.RSCMHandler
import dev.openrune.cache.tools.tasks.CacheTask
import dev.openrune.cache.tools.tasks.TaskType
import java.io.File
import java.nio.file.Path

val DEFAULT_PATH = Path.of("data", "cache").toFile()

data class TokenizedReplacement(val key : String, val value : String)

data class Builder(
    var type: TaskType,
    var revision: Int,
    var cacheLocation : File = DEFAULT_PATH,
    var extraTasks: Array<CacheTask> = emptyArray()
) {

    val tokenizedReplacement = emptyList<TokenizedReplacement>().toMutableList()

    fun extraTasks(vararg types: CacheTask) = apply { this.extraTasks = types.toMutableList().toTypedArray() }

    fun registerRSCM(mappingsDir: File) = apply {
        RSCMHandler.load(mappingsDir)
    }

    fun registerTokenizedReplacement(vararg replacement: TokenizedReplacement) = apply {
        tokenizedReplacement.addAll(replacement)
    }


    fun cacheLocation(cacheLocation: File) = apply { this.cacheLocation = cacheLocation }

    fun build() = CacheTool(this)
}