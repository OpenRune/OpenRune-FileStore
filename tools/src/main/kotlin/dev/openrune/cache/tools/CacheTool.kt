package dev.openrune.cache.tools

import com.github.michaelbull.logging.InlineLogger
import dev.openrune.cache.tools.tasks.CacheTask
import dev.openrune.cache.tools.tasks.TaskType
import dev.openrune.cache.tools.tasks.impl.PackGameVals
import dev.openrune.definition.Js5GameValGroup
import java.io.File

class CacheTool(
    val type: TaskType,
    val revision: Int,
    val cacheLocation: File,
    val extraTasks: List<CacheTask> = emptyList()
) {
    private val logger = InlineLogger()

    companion object {
        val gameValMappings: MutableMap<Js5GameValGroup, MutableList<Pair<String, Int>>> = mutableMapOf()

        fun addGameValMapping(type: Js5GameValGroup, key: String, value: Int) {
            val list = gameValMappings.getOrPut(type) { mutableListOf() }
            list.add(key to value)
            gameValMappings[type] = list
        }
    }

    fun initialize() {
        if (cacheLocation == DEFAULT_PATH) {
            logger.info { "Using Default path of [${DEFAULT_PATH.absolutePath}]" }
        }

        val sortedTasks = extraTasks.sortedBy { it.priority.priorityValue }

        when (type) {
            TaskType.BUILD -> {
                BuildCache(
                    input = cacheLocation,
                    tasks = sortedTasks.toMutableList(),
                    revision = revision,
                ).initialize()
            }
            TaskType.FRESH_INSTALL -> {
                FreshCache(
                    downloadLocation = cacheLocation,
                    tasks = sortedTasks.toMutableList(),
                    revision = revision
                ).initialize()
            }
        }
    }
}