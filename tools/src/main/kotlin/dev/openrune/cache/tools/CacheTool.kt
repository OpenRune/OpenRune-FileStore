package dev.openrune.cache.tools

import com.github.michaelbull.logging.InlineLogger
import dev.openrune.cache.gameval.GameValElement
import dev.openrune.cache.tools.incremental.CacheVerification
import dev.openrune.cache.tools.incremental.IncrementalSession
import dev.openrune.cache.tools.progress.CacheProgress
import dev.openrune.cache.tools.progress.DefaultCacheProgress
import dev.openrune.cache.tools.tasks.CacheTask
import dev.openrune.cache.tools.tasks.TaskType
import dev.openrune.definition.GameValGroupTypes
import java.io.File

class CacheTool(
    val type: TaskType,
    val revision: Int,
    val subRevision: Int = -1,
    val cacheEnvironment: CacheEnvironment = CacheEnvironment.LIVE,
    val cacheLocation: File,
    val serverCacheLocation: File? = null,
    val extraTasks: List<CacheTask> = emptyList(),
    val autoCertIds: Map<String, Int> = emptyMap(),
    val incremental: Boolean = true,
    val incrementalDatabase: File? = null,
    val verification: CacheVerification = CacheVerification.FINGERPRINT,
    val progress: CacheProgress = DefaultCacheProgress()
) {
    private val logger = InlineLogger()

    companion object {
        val gameValMappings: MutableMap<GameValGroupTypes, MutableList<GameValElement>> = mutableMapOf()

        @Volatile
        var gameValListener: ((GameValGroupTypes, GameValElement) -> Unit)? = null

        fun addGameValMapping(type: GameValGroupTypes, element: GameValElement) {
            val list = gameValMappings.getOrPut(type) { mutableListOf() }
            list.add(element)
            gameValListener?.invoke(type, element)
        }
    }

    fun initialize() {
        if (cacheLocation == DEFAULT_PATH) {
            logger.info { "Using default path: ${DEFAULT_PATH.absolutePath}" }
        }

        val sortedTasks = extraTasks
            .sortedBy { it.priority.priorityValue }
            .filter { !(it.serverTaskOnly && type != TaskType.SERVER_CACHE_BUILD) }

        if (type == TaskType.SERVER_CACHE_BUILD) {
            val serverDir = serverCacheLocation ?: error("Please define serverCacheLocation for SERVER_CACHE_BUILD")
            if (!serverDir.exists()) serverDir.mkdirs()

            val stateDir = IncrementalSession.stateDirectory(cacheLocation, incrementalDatabase)
            cacheLocation.listFiles()?.forEach { file ->
                if (file.absoluteFile == stateDir) return@forEach
                file.copyTo(File(serverDir, file.name), overwrite = true)
            }
        }

        when (type) {
            TaskType.BUILD, TaskType.SERVER_CACHE_BUILD -> {
                BuildCache(
                    cacheLocation = if (type == TaskType.BUILD) cacheLocation else serverCacheLocation!!,
                    serverPass = type == TaskType.SERVER_CACHE_BUILD,
                    tasks = sortedTasks.toMutableList(),
                    revision = revision,
                    incremental = incremental,
                    incrementalDatabase = incrementalDatabase,
                    verification = verification,
                    progress = progress
                ).initialize()
            }

            TaskType.FRESH_INSTALL -> {
                require(revision != -1) { "Unable to detect cache revision — set it manually via .revision(id)." }

                File(cacheLocation, "xteas.json").delete()

                // A fresh install replaces the cache wholesale, so nothing recorded about the old one is
                // worth keeping. Cleared explicitly rather than left to the fingerprint check, so state
                // configured outside the cache directory is discarded too.
                IncrementalSession.clearState(cacheLocation, incrementalDatabase)

                FreshCache(
                    cacheOutput = cacheLocation,
                    tasks = sortedTasks.toMutableList(),
                    revision = revision,
                    subRev = subRevision,
                    cacheEnvironment = cacheEnvironment,
                    progress = progress
                ).initialize()
            }
        }
    }
}