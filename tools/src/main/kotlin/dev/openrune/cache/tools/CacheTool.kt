package dev.openrune.cache.tools

import dev.openrune.cache.tools.tasks.TaskType
import dev.openrune.cache.tools.tasks.impl.PackGameVals
import dev.openrune.definition.Js5GameValGroup
import io.github.oshai.kotlinlogging.KotlinLogging

class CacheTool(private val builder: Builder) {

    private val logger = KotlinLogging.logger {}
    companion object {
        val gameValMappings: MutableMap<Js5GameValGroup, MutableList<Pair<String, Int>>> = mutableMapOf()

        fun addGameValMapping(type: Js5GameValGroup, key: String, value: Int) {
            val list = gameValMappings.getOrPut(type) { mutableListOf() }
            list.add(Pair(key, value))
            gameValMappings[type] = list
        }
    }

    fun initialize() {

        if (builder.cacheLocation == DEFAULT_PATH) logger.info { "Using Default path of [${DEFAULT_PATH.absolutePath}]" }

        when(builder.type) {
            TaskType.BUILD -> {
                BuildCache(
                    input = builder.cacheLocation,
                    tasks = builder.extraTasks.toMutableList().apply {
                        add(PackGameVals(builder.revision))
                    }.sortedBy { it.priority.priorityValue }.toMutableList()
                ).initialize()
            }
            TaskType.FRESH_INSTALL -> {
                FreshCache(
                    downloadLocation = builder.cacheLocation,
                    tasks = builder.extraTasks.toMutableList().apply {
                        add(PackGameVals(builder.revision))
                    }.sortedBy { it.priority.priorityValue }.toMutableList(),
                    revision = builder.revision,
                    removeXteas = builder.removeXteas(),
                    removeBzip = builder.removeBzip(),
                ).initialize()
            }
        }
    }

}