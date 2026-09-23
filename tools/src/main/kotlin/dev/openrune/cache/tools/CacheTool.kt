package dev.openrune.cache.tools

import com.displee.cache.CacheLibrary
import com.github.michaelbull.logging.InlineLogger
import dev.openrune.cache.gameval.GameValElement
import dev.openrune.cache.tools.gameval.GameValPublisher
import dev.openrune.cache.tools.incremental.CacheTarget
import dev.openrune.cache.tools.incremental.CacheVerification
import dev.openrune.cache.tools.incremental.IncrementalSession
import dev.openrune.cache.tools.incremental.PackState
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
    val progress: CacheProgress = DefaultCacheProgress(),
    /**
     * Indices a server cache does not need (models, music, textures...). When set, the server cache is
     * produced by rebuilding the live cache straight into [serverCacheLocation] with these indices left
     * empty, instead of copying every file and stripping them afterwards.
     */
    val serverEmptyIndices: Set<Int> = emptySet(),
    /**
     * Seed the server cache by rebuilding the live one sector by sector, so the emptied indices' data is
     * gone from the main file too and the server cache is as small as it can be. Set to false to trade
     * that for a faster plain file copy, where the emptied data lingers in the main file.
     */
    val serverCompact: Boolean = true,
) {
    private val logger = InlineLogger()

    companion object {
        val gameValMappings: MutableMap<GameValGroupTypes, MutableList<GameValElement>> = mutableMapOf()

        @Volatile
        var gameValListener: ((GameValGroupTypes, GameValElement) -> Unit)? = null

        /**
         * Records a gameval for [dev.openrune.cache.tools.tasks.impl.PackGameVals] to encode and publishes it
         * to the constant provider straight away, so tasks later in the build resolve the id being packed.
         */
        fun addGameValMapping(type: GameValGroupTypes, element: GameValElement) {
            val list = gameValMappings.getOrPut(type) { mutableListOf() }
            list.add(element)
            GameValPublisher.publish(type, element)
            gameValListener?.invoke(type, element)
        }
    }

    /**
     * Makes [serverDir] a copy of the live cache for the server pass to build on, with [serverEmptyIndices]
     * emptied. By default ([serverCompact]) the live cache is rebuilt into [serverDir] without those
     * indices, so the result is compact. Otherwise it is a plain file copy plus clearing the indices in
     * place, which is quicker but leaves the emptied data in the main file. The incremental state
     * directory is never touched, so the server's own records survive.
     */
    private fun seedServerCache(serverDir: File) {
        val preserved = if (incremental) serverOutputs(serverDir) else emptyMap()
        reseedServerCache(serverDir)
        restoreServerOutputs(serverDir, preserved)
    }

    /**
     * Everything the previous server passes wrote, read from the old server cache before it is replaced.
     * Reseeding from the live cache would otherwise drop these entries, and the incremental engine would
     * repack every server-only definition on every build because its outputs had vanished.
     */
    private fun serverOutputs(serverDir: File): Map<CacheTarget, ByteArray> {
        val database = incrementalDatabase ?: IncrementalSession.defaultDatabase(serverDir)
        if (!database.isFile || !File(serverDir, "${CacheLibrary.CACHE_FILE_NAME}.dat2").isFile) return emptyMap()
        val targets = PackState.open(database)?.use { it.allOutputs() } ?: return emptyMap()
        if (targets.isEmpty()) return emptyMap()

        val preserved = LinkedHashMap<CacheTarget, ByteArray>()
        runCatching {
            CacheLibrary(serverDir.absolutePath).use { server ->
                targets.forEach { target ->
                    if (target.file < 0 || !server.exists(target.index)) return@forEach
                    server.data(target.index, target.archive, target.file)?.let { preserved[target] = it }
                }
            }
        }.onFailure { logger.warn(it) { "Could not read the previous server cache; server-only definitions will be repacked" } }
        return preserved
    }

    private fun restoreServerOutputs(serverDir: File, preserved: Map<CacheTarget, ByteArray>) {
        if (preserved.isEmpty()) return
        CacheLibrary(serverDir.absolutePath).use { server ->
            preserved.forEach { (target, data) ->
                if (!server.exists(target.index)) return@forEach
                server.put(target.index, target.archive, target.file, data)
            }
            server.update()
        }
        logger.debug { "Carried ${preserved.size} server-only cache entries across the reseed" }
    }

    private fun reseedServerCache(serverDir: File) {
        val stateDir = IncrementalSession.stateDirectory(cacheLocation, incrementalDatabase)
        val serverStateDir = IncrementalSession.stateDirectory(serverDir, incrementalDatabase)

        if (serverCompact && serverEmptyIndices.isNotEmpty()) {
            logger.info { "Seeding server cache from the live cache (compacting, ${serverEmptyIndices.size} indices left empty)" }
            // A rebuild appends to whatever is already there, so the previous server cache files go first.
            serverDir.listFiles()?.forEach { file ->
                if (file.absoluteFile == serverStateDir) return@forEach
                if (file.isFile && file.name.startsWith(CacheLibrary.CACHE_FILE_NAME)) file.delete()
            }
            CacheLibrary(cacheLocation.absolutePath).use { live ->
                live.rebuild(serverDir.toPath(), serverEmptyIndices)
            }
            // Anything beside the cache files (xteas.json, say) still comes across.
            cacheLocation.listFiles()?.forEach { file ->
                if (file.absoluteFile == stateDir || !file.isFile) return@forEach
                if (file.name.startsWith(CacheLibrary.CACHE_FILE_NAME)) return@forEach
                file.copyTo(File(serverDir, file.name), overwrite = true)
            }
            return
        }

        cacheLocation.listFiles()?.forEach { file ->
            if (file.absoluteFile == stateDir) return@forEach
            file.copyTo(File(serverDir, file.name), overwrite = true)
        }
        if (serverEmptyIndices.isEmpty()) return

        CacheLibrary(serverDir.absolutePath).use { server ->
            serverEmptyIndices.forEach { id ->
                if (!server.exists(id)) return@forEach
                val index = server.index(id)
                if (index.archiveIds().isEmpty()) return@forEach
                index.clear()
                // clear() alone does not mark the reference table dirty, so update() would write nothing.
                index.flag()
                index.update()
            }
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

            seedServerCache(serverDir)
        }

        when (type) {
            TaskType.BUILD, TaskType.SERVER_CACHE_BUILD -> {
                BuildCache(
                    cacheLocation = if (type == TaskType.BUILD) cacheLocation else serverCacheLocation!!,
                    serverPass = type == TaskType.SERVER_CACHE_BUILD,
                    tasks = sortedTasks.toMutableList(),
                    revision = revision,
                    subRevision = subRevision,
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