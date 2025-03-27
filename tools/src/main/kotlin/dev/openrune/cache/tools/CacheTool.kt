package dev.openrune.cache.tools

import dev.openrune.cache.tools.tasks.TaskType
import io.github.oshai.kotlinlogging.KotlinLogging

class CacheTool(private val builder: Builder) {

    private val logger = KotlinLogging.logger {}

    fun initialize() {

        if (builder.cacheLocation == DEFAULT_PATH) logger.info { "Using Default path of [${DEFAULT_PATH.absolutePath}]" }

        when(builder.type) {
            TaskType.BUILD -> {
                BuildCache(
                    input = builder.cacheLocation,
                    tasks = builder.extraTasks.toMutableList()
                ).initialize()
            }
            TaskType.FRESH_INSTALL -> {
                FreshCache(
                    downloadLocation = builder.cacheLocation,
                    tasks = builder.extraTasks.toMutableList(),
                    revision = builder.revision,
                    removeXteas = builder.removeXteas(),
                    removeBzip = builder.removeBzip(),
                ).initialize()
            }
        }
    }

}