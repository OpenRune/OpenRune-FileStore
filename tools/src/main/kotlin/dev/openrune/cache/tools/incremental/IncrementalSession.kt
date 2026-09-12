package dev.openrune.cache.tools.incremental

import com.github.michaelbull.logging.InlineLogger
import java.io.File

class IncrementalSession private constructor(
    private val state: PackState?,
    val build: IncrementalBuild,
    private val revision: Int,
    private val scope: String = "",
) : AutoCloseable {
    private val logger = InlineLogger()

    val enabled: Boolean get() = build.enabled

    fun finish(versionTable: ByteArray) {
        val store = state ?: return
        runCatching {
            store.putMeta("$META_SCHEMA$scope", PackState.SCHEMA_VERSION)
            store.putMeta("$META_REVISION$scope", revision.toString())
            store.putMeta("$META_VERSION_TABLE$scope", Hashing.hashBytes(versionTable))
            store.commit()
        }.onFailure { logger.warn(it) { "Could not persist incremental pack state" } }
    }

    override fun close() {
        state?.close()
    }

    companion object {
        private const val META_SCHEMA = "schema"
        private const val META_REVISION = "revision"
        private const val META_VERSION_TABLE = "versionTable"

        const val FULL_REBUILD_ENV = "OPENRUNE_FULL_REBUILD"

        private val logger = InlineLogger()

        const val DIRECTORY_NAME = "incremental"

        private const val DATABASE_NAME = "packstate.db"

        fun defaultDatabase(cacheLocation: File): File =
            File(File(cacheLocation.absoluteFile, DIRECTORY_NAME), DATABASE_NAME)

        /**
         * Directory holding the state database, honouring [databaseOverride]. Cache cleanup must leave this
         * directory alone, otherwise wiping the cache silently discards the build state with it.
         */
        fun stateDirectory(cacheLocation: File, databaseOverride: File?): File =
            (databaseOverride ?: defaultDatabase(cacheLocation)).absoluteFile.parentFile
                ?: File(cacheLocation.absoluteFile, DIRECTORY_NAME)

        /**
         * Discards all recorded state, so the next build packs everything. Used when the cache is replaced
         * wholesale and nothing recorded about the previous one can apply.
         */
        fun clearState(cacheLocation: File, databaseOverride: File?) {
            val database = (databaseOverride ?: defaultDatabase(cacheLocation)).absoluteFile
            val directory = database.parentFile

            // SQLite keeps -wal/-shm siblings; leaving them behind would resurrect a partial database.
            val removed = if (directory != null && directory.name == DIRECTORY_NAME) {
                directory.deleteRecursively()
            } else {
                listOf("", "-wal", "-shm", "-journal")
                    .map { File(database.parentFile, database.name + it) }
                    .filter { it.exists() }
                    .map { it.delete() }
                    .any { it }
            }

            if (removed) logger.info { "Incremental packing: cleared state at $database" }
        }

        fun open(
            enabled: Boolean,
            cacheLocation: File,
            databaseOverride: File?,
            revision: Int,
            versionTable: ByteArray,
            verification: CacheVerification = CacheVerification.FINGERPRINT,
        ): IncrementalSession {
            if (!enabled) return IncrementalSession(null, IncrementalBuild.DISABLED, revision)

            val forcedByEnv = System.getenv(FULL_REBUILD_ENV)?.lowercase() in setOf("1", "true", "yes")
            val database = databaseOverride ?: defaultDatabase(cacheLocation)
            val state = PackState.open(database)
                ?: return IncrementalSession(null, IncrementalBuild.DISABLED, revision)

            state.beginBuild()

            // Meta is namespaced by cache location so pointing two caches at one overridden database does
            // not make each build look like the other one modified the cache.
            val scope = ":" + Hashing
                .hashBytes(cacheLocation.absolutePath.replace('\\', '/').toByteArray())
                .take(12)

            val reset = when {
                forcedByEnv -> "$FULL_REBUILD_ENV is set"
                state.meta("$META_SCHEMA$scope") != PackState.SCHEMA_VERSION -> "state schema changed"
                state.meta("$META_REVISION$scope") != revision.toString() -> "cache revision changed"
                verification == CacheVerification.FINGERPRINT &&
                    state.meta("$META_VERSION_TABLE$scope") != Hashing.hashBytes(versionTable) ->
                    "cache was modified outside the packer"
                else -> null
            }

            if (reset != null) {
                logger.info { "Incremental packing: full repack ($reset)" }
                runCatching { state.clearUnits() }
            } else {
                logger.info { "Incremental packing enabled (state: ${state.file})" }
            }

            return IncrementalSession(
                state,
                IncrementalBuild(state, forced = false, verification = verification),
                revision,
                scope,
            )
        }
    }
}
