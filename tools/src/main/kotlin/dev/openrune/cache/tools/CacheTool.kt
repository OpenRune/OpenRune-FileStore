package dev.openrune.cache.tools

import com.github.michaelbull.logging.InlineLogger
import dev.openrune.cache.gameval.GameValElement
import dev.openrune.cache.tools.tasks.CacheTask
import dev.openrune.cache.tools.tasks.TaskType
import dev.openrune.definition.GameValGroupTypes
import java.io.File

class CacheTool(
    val type: TaskType,
    val revision: Int,
    val cacheLocation: File,
    val extraTasks: List<CacheTask> = emptyList()
) {
    private val logger = InlineLogger()

    companion object {
        val gameValMappings: MutableMap<GameValGroupTypes, MutableList<GameValElement>> = mutableMapOf()

        fun addGameValMapping(type: GameValGroupTypes, element: GameValElement) {
            val list = gameValMappings.getOrPut(type) { mutableListOf() }
            list.add(element)
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
                File(cacheLocation,"xteas.json").delete()
                FreshCache(
                    downloadLocation = cacheLocation,
                    tasks = sortedTasks.toMutableList(),
                    revision = revision
                ).initialize()
            }
        }
    }
}