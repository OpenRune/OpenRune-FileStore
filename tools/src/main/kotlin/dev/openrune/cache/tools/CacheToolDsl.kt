package dev.openrune.cache.tools

import dev.openrune.cache.tools.tasks.CacheTask
import dev.openrune.cache.tools.tasks.TaskType
import dev.openrune.cache.tools.tasks.impl.PackGameVals
import dev.openrune.cache.tools.tasks.impl.RemoveBzip
import dev.openrune.cache.tools.tasks.impl.RemoveXteas
import dev.openrune.definition.constants.ConstantProvider
import java.io.File

fun cacheTool(block: CacheToolDsl.() -> Unit): CacheTool {
    return CacheToolDsl().apply(block).build()
}

class CacheToolDsl {
    var taskType: TaskType? = null
    var revision: Int? = null
    private var cache: File? = null

    private val addedTasks = mutableListOf<CacheTask>()
    private val removedTasks = mutableListOf<CacheTask>()

    private var rscmDir: File? = null

    fun tasks(block: TaskBuilder.() -> Unit) {
        TaskBuilder().apply(block).also {
            addedTasks += it.addedTasks
            removedTasks += it.removedTasks
        }
    }

    fun cache(path: String) {
        cache = File(path)
    }

    fun rscm(path: String) {
        rscmDir = File(path)
    }

    fun build(): CacheTool {
        val type = taskType ?: error("`taskType` must be set")
        val revision = revision ?: error("`revision` must be set")
        val cacheLocation = cache ?: error("`cache` must be set")

        rscmDir?.let(ConstantProvider::load)

        val defaultGameVals = PackGameVals(revision)

        if (removedTasks.none { it is PackGameVals }) {
            addedTasks += defaultGameVals
        }

        if (type == TaskType.FRESH_INSTALL) {
            val xteasFile = File(cacheLocation, "xteas.json")
            if (removedTasks.none { it is RemoveXteas}) {
                addedTasks += RemoveXteas(xteasFile)
            }
            if (removedTasks.none { it is RemoveBzip }) {
                addedTasks += RemoveBzip()
            }
        }

        val finalTasks = addedTasks.filter { added ->
            removedTasks.none { removed -> removed::class == added::class }
        }

        val cleanedTasks = finalTasks.filterNot { it is RemoveXteas || it is RemoveBzip }

        return CacheTool(
            type = type,
            revision = revision,
            cacheLocation = cacheLocation,
            extraTasks = cleanedTasks
        )
    }

    class TaskBuilder {
        internal val addedTasks = mutableListOf<CacheTask>()
        internal val removedTasks = mutableListOf<CacheTask>()

        operator fun CacheTask.unaryPlus() {
            addedTasks += this
        }

        operator fun CacheTask.unaryMinus() {
            removedTasks += this
        }
    }
}
