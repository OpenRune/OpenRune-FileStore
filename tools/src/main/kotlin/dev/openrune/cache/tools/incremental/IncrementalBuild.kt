package dev.openrune.cache.tools.incremental

import com.github.michaelbull.logging.InlineLogger
import dev.openrune.cache.GAMEVALS
import dev.openrune.cache.gameval.GameValElement
import dev.openrune.cache.gameval.impl.Sprite
import dev.openrune.cache.tools.CacheTool
import dev.openrune.cache.tools.incremental.Hashing.invariantPath
import dev.openrune.cache.tools.tasks.CacheTask
import dev.openrune.cache.util.progress
import dev.openrune.definition.GameValGroupTypes
import dev.openrune.definition.constants.ConstantProvider
import dev.openrune.filesystem.Cache
import java.io.File
import java.util.zip.CRC32

class IncrementalBuild internal constructor(
    private val state: PackState?,
    private val forced: Boolean,
    private val verification: CacheVerification = CacheVerification.FINGERPRINT,
) {
    private val logger = InlineLogger()

    private val writtenThisBuild = HashSet<CacheTarget>()
    private val presence = HashMap<Long, Set<Int>>()

    val enabled: Boolean get() = state != null && !forced

    fun run(
        task: CacheTask,
        scope: String,
        label: String,
        cache: Cache,
        units: List<PackUnit>,
        extraDeps: List<File> = emptyList(),
        extraFingerprints: Map<String, String> = emptyMap(),
        showProgress: Boolean = true,
        pack: (Cache, PackUnit) -> Unit,
    ) {
        val store = state
        if (store == null || forced) {
            packAll(task, label, cache, units, showProgress, pack)
            return
        }

        val taskKey = "${task::class.java.name}|$scope|server=${task.serverPass}"
        val stored = store.loadTask(taskKey)
        val extraDepHashes =
            extraDeps.associate { it.invariantPath() to Hashing.hashTree(it) } + extraFingerprints
        val hashes = units.associate { it.key to (it.fingerprint ?: Hashing.hashFiles(it.sources)) }

        val dirty = selectDirty(cache, units, stored, hashes, extraDepHashes)
        val skipped = units.filterNot { it.key in dirty }

        pruneRemoved(cache, store, taskKey, stored, units.map { it.key }.toSet())

        skipped.forEach { unit -> replay(stored.getValue(unit.key)) }

        task.progress.summary(label, dirty.size, skipped.size)

        if (dirty.isEmpty()) return

        val recording = RecordingCache(cache)
        val progress = if (showProgress) task.progress.begin(label, dirty.size) else null

        units.filter { it.key in dirty }.forEach { unit ->
            progress?.message(unit.label)
            val record = UnitRecord()
            recording.record = record
            val outcome = withRecorders(record) { runCatching { pack(recording, unit) } }
            recording.record = null
            writtenThisBuild.addAll(record.outputs.keys)

            outcome.onFailure { failure ->

                logger.warn(failure) { "$label: failed to pack ${unit.label}; it will be repacked next build" }
                runCatching { store.deleteUnits(taskKey, listOf(unit.key)) }
            }.onSuccess {
                pruneStaleOutputs(cache, stored, unit.key, record)
                runCatching { store.saveUnit(taskKey, unit.key, hashes.getValue(unit.key), record, extraDepHashes) }
                    .onFailure { logger.warn(it) { "$label: could not record state for ${unit.label}" } }
            }
            progress?.step()
        }

        progress?.close()
    }

    fun runOnce(
        task: CacheTask,
        scope: String,
        label: String,
        cache: Cache,
        fingerprint: String,
        sources: List<File> = emptyList(),
        pack: (Cache) -> Unit,
    ) = run(
        task = task,
        scope = scope,
        label = label,
        cache = cache,
        units = listOf(PackUnit(key = "all", sources = sources, label = label, fingerprint = fingerprint)),
        showProgress = false,
    ) { packCache, _ -> pack(packCache) }

    private fun packAll(
        task: CacheTask,
        label: String,
        cache: Cache,
        units: List<PackUnit>,
        showProgress: Boolean,
        pack: (Cache, PackUnit) -> Unit,
    ) {
        if (units.isEmpty()) return
        val progress = if (showProgress) task.progress.begin(label, units.size) else null
        units.forEach { unit ->
            progress?.message(unit.label)
            pack(cache, unit)
            progress?.step()
        }
        progress?.close()
    }

    private fun pruneStaleOutputs(
        cache: Cache,
        stored: Map<String, StoredUnit>,
        key: String,
        record: UnitRecord,
    ) {
        val previous = stored[key] ?: return
        val claimed = stored.asSequence()
            .filter { it.key != key }
            .flatMapTo(HashSet()) { it.value.outputs.keys.asSequence() }

        val gone = previous.outputs.keys.asSequence()
            .filterNot { it in record.outputs || it in writtenThisBuild || it in claimed }
            .toList()

        gone.forEach { target ->
            runCatching { cache.remove(target.index, target.archive, target.file) }
                .onFailure { logger.warn(it) { "Could not remove stale cache entry $target" } }
        }
        if (gone.isNotEmpty()) {
            logger.info { "Removed ${gone.size} cache entries no longer produced by $key" }
        }

        val claimedGameVals = stored.asSequence()
            .filter { it.key != key }
            .flatMapTo(HashSet()) { entry -> entry.value.gameVals.mapNotNull(::gameValTarget) }
        val stillEmitted = record.gameVals.mapNotNullTo(HashSet(), ::gameValTarget)

        removeGameVals(
            cache,
            previous.gameVals.asSequence()
                .mapNotNull(::gameValTarget)
                .filterNot { it in stillEmitted || it in claimedGameVals }
                .toSet(),
        )
    }

    /**
     * Cache slot a gameval registration occupies. Renaming or renumbering a definition leaves the old slot
     * behind otherwise, and the gameval table then reports one name at two ids — which breaks the CS2
     * compile, since Neptune rejects duplicate symbols.
     */
    private fun gameValTarget(emit: GameValEmit): CacheTarget? =
        runCatching { GameValGroupTypes.valueOf(emit.group) }.getOrNull()
            ?.let { CacheTarget(GAMEVALS, it.id, emit.id) }

    /**
     * Safe to do eagerly: PackGameVals runs at the end of the build and rewrites every registration still
     * live, so removing one that turns out to still be in use only costs it being written again.
     */
    private fun removeGameVals(cache: Cache, targets: Set<CacheTarget>) {
        if (targets.isEmpty()) return
        targets.forEach { target ->
            runCatching { cache.remove(target.index, target.archive, target.file) }
                .onFailure { logger.warn(it) { "Could not remove stale gameval $target" } }
        }
        logger.info { "Removed ${targets.size} stale gameval entries" }
    }

    private fun selectDirty(
        cache: Cache,
        units: List<PackUnit>,
        stored: Map<String, StoredUnit>,
        hashes: Map<String, String>,
        extraDepHashes: Map<String, String>,
    ): Set<String> {
        val dirty = LinkedHashSet<String>()

        units.forEach { unit ->
            val previous = stored[unit.key]
            val reason = when {
                previous == null -> "new"
                previous.hash != hashes[unit.key] -> "source changed"
                previous.extraFiles != extraDepHashes -> "shared dependency changed"
                changedConstant(previous) != null -> "gameval ${changedConstant(previous)} changed"
                previous.outputs.keys.any { it in writtenThisBuild } -> "output rewritten by an earlier task"
                previous.cacheReads.any { readInvalidated(it) } -> "depends on a cache entry rewritten this build"
                staleOutput(cache, previous) != null -> "cache no longer holds ${staleOutput(cache, previous)}"
                else -> null
            }
            if (reason != null) {
                dirty += unit.key
                // Enable dev.openrune.cache.tools.incremental at DEBUG to see why each unit was repacked.
                logger.debug { "dirty: ${unit.label} [${unit.key}] ($reason)" }
            }
        }

        propagate(units, stored, dirty)
        return dirty
    }

    /**
     * First recorded output the cache no longer holds as this unit left it, or null when all still match.
     * Under FINGERPRINT only presence is checked, since a matching whole-cache fingerprint already means
     * the contents are untouched. Under OUTPUT_CRC the payload is compared, which is what lets a cache
     * that was reseeded between builds keep the entries that happen to be correct already.
     */
    private fun staleOutput(cache: Cache, previous: StoredUnit): CacheTarget? =
        previous.outputs.entries.firstOrNull { (target, crc) ->
            when {
                target.file == RecordingCache.ANY_FILE -> false
                !presentInCache(cache, target) -> true
                verification == CacheVerification.FINGERPRINT -> false
                else -> crcOf(cache, target) != crc
            }
        }?.key

    private fun crcOf(cache: Cache, target: CacheTarget): Int? {
        val data = runCatching { cache.data(target.index, target.archive, target.file) }.getOrNull()
            ?: return null
        return CRC32().apply { update(data) }.value.toInt()
    }

    private fun changedConstant(previous: StoredUnit): String? =
        previous.constants.entries.firstOrNull { (key, value) -> ConstantProvider.peekMapping(key) != value }?.key

    private fun readInvalidated(read: CacheTarget): Boolean {
        if (read in writtenThisBuild) return true
        if (read.file != RecordingCache.ANY_FILE) return false
        return writtenThisBuild.any { it.index == read.index && it.archive == read.archive }
    }

    private fun propagate(units: List<PackUnit>, stored: Map<String, StoredUnit>, dirty: MutableSet<String>) {
        val byOutput = HashMap<CacheTarget, MutableList<String>>()
        stored.forEach { (key, unit) ->
            unit.outputs.keys.forEach { target -> byOutput.getOrPut(target) { mutableListOf() } += key }
        }
        val present = units.mapTo(HashSet()) { it.key }

        var changed = true
        while (changed) {
            changed = false
            val dirtyOutputs = dirty.mapNotNull { stored[it] }.flatMapTo(HashSet()) { it.outputs.keys }

            dirtyOutputs.forEach { target ->
                byOutput[target]?.forEach { key ->
                    if (key in present && dirty.add(key)) changed = true
                }
            }

            stored.forEach { (key, unit) ->
                if (key in dirty || key !in present) return@forEach
                val touched = unit.cacheReads.any { read ->
                    read in dirtyOutputs ||
                        (read.file == RecordingCache.ANY_FILE &&
                            dirtyOutputs.any { it.index == read.index && it.archive == read.archive })
                }
                if (touched && dirty.add(key)) changed = true
            }
        }
    }

    private fun pruneRemoved(
        cache: Cache,
        store: PackState,
        taskKey: String,
        stored: Map<String, StoredUnit>,
        currentKeys: Set<String>,
    ) {
        val removed = stored.keys - currentKeys
        if (removed.isEmpty()) return

        val claimed = stored.filterKeys { it in currentKeys }
            .values
            .flatMapTo(HashSet()) { it.outputs.keys }

        val orphans = removed.asSequence()
            .flatMap { stored.getValue(it).outputs.keys.asSequence() }
            .filterNot { it in claimed || it in writtenThisBuild }
            .toSet()

        orphans.forEach { target ->
            runCatching { cache.remove(target.index, target.archive, target.file) }
                .onFailure { logger.warn(it) { "Could not remove stale cache entry $target" } }
        }
        if (orphans.isNotEmpty()) {
            logger.info { "Removed ${orphans.size} cache entries for ${removed.size} deleted source(s)" }
        }

        val claimedGameVals = stored.filterKeys { it in currentKeys }
            .values
            .flatMapTo(HashSet()) { unit -> unit.gameVals.mapNotNull(::gameValTarget) }

        removeGameVals(
            cache,
            removed.asSequence()
                .flatMap { stored.getValue(it).gameVals.asSequence() }
                .mapNotNull(::gameValTarget)
                .filterNot { it in claimedGameVals }
                .toSet(),
        )

        store.deleteUnits(taskKey, removed)
    }

    private inline fun <T> withRecorders(record: UnitRecord, body: () -> T): T {
        ConstantProvider.lookupListener = { key, value -> record.constants[key] = value }
        CacheTool.gameValListener = { group, element -> record.gameVals += element.toEmit(group) }
        try {
            return body()
        } finally {
            ConstantProvider.lookupListener = null
            CacheTool.gameValListener = null
        }
    }

    private fun replay(unit: StoredUnit) {
        unit.gameVals.forEach { emit ->
            val group = runCatching { GameValGroupTypes.valueOf(emit.group) }.getOrNull() ?: return@forEach
            val element = when (emit.kind) {
                SPRITE_KIND -> Sprite(emit.name, emit.subId, emit.id)
                else -> GameValElement(emit.name, emit.id)
            }
            CacheTool.addGameValMapping(group, element)
        }
    }

    private fun GameValElement.toEmit(group: GameValGroupTypes): GameValEmit = when (this) {
        is Sprite -> GameValEmit(group.name, SPRITE_KIND, name, id, index)
        else -> GameValEmit(group.name, BASE_KIND, name, id, -1)
    }

    private fun presentInCache(cache: Cache, target: CacheTarget): Boolean {
        if (target.file == RecordingCache.ANY_FILE) return true
        val key = (target.index.toLong() shl 32) or (target.archive.toLong() and 0xFFFFFFFFL)
        val files = presence.getOrPut(key) {
            runCatching { cache.files(target.index, target.archive).toSet() }.getOrDefault(emptySet())
        }
        return target.file in files
    }

    internal fun invalidatePresence() = presence.clear()

    companion object {
        private const val BASE_KIND = "base"
        private const val SPRITE_KIND = "sprite"

        val DISABLED = IncrementalBuild(null, forced = true)
    }
}
