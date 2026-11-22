package dev.openrune.cache.tools.gameval

import com.github.michaelbull.logging.InlineLogger
import dev.openrune.cache.CONFIGS
import dev.openrune.cache.tools.incremental.ConfigKey
import dev.openrune.cache.tools.incremental.ConfigRef
import dev.openrune.cache.tools.incremental.IncrementalBuild
import dev.openrune.cache.tools.incremental.StoredConfigRefs
import dev.openrune.cache.tools.progress.CacheProgress
import dev.openrune.cache.tools.progress.DefaultCacheProgress
import dev.openrune.definition.type.ParamType
import dev.openrune.filesystem.Cache
import java.io.File
import java.util.zip.CRC32

/**
 * Keeps cache configs pointing at the right gameval ids across builds. Driven by
 * [dev.openrune.cache.tools.BuildCache] itself, not by a task, so it is always part of a build.
 *
 * [index] runs before any task. The first time a cache is seen (or after the incremental state was
 * reset) every enum, struct, item, npc, loc, dbrow and dbtable is decoded and each gameval-typed
 * value recorded by name, using the cache's own gameval index so the record matches the config bytes.
 *
 * [relink] runs after every task. For each indexed config:
 * - gone from the cache: its record is dropped;
 * - bytes changed since last seen (repacked from TOML or by another task): its references are
 *   re-read from the new bytes, so the index follows whatever now owns the config;
 * - otherwise, every reference whose name now resolves to a different id is rewritten in place and
 *   the config re-encoded. An enum pointing at `component.bank:items` keeps pointing at that
 *   component after an `insertAfter` shifts its index, and a struct pointing at a toplevel interface
 *   follows the interface when its id is redeclared, with no TOML copy of either config needed.
 *
 * Configs first written this build by a recorded task are added to the index as well. The index lives
 * in the incremental state database, so with incremental packing off this does nothing.
 */
internal object GameValReferenceIndex {

    private val logger = InlineLogger()

    private const val MIN_REVISION = 230
    private const val SAMPLE = 5

    fun index(cache: Cache, revision: Int, incremental: IncrementalBuild, progress: CacheProgress) {
        if (revision < MIN_REVISION) return
        val store = incremental.store ?: return
        if (store.hasConfigRefIndex()) return

        val names = GameValReferences.namesFromCache(cache, revision)
        val knownTables = GameValReferences.knownTables(names)
        val paramTypes = GameValReferences.loadParamTypes(cache, revision)

        val work = ConfigRefKind.entries.associateWith { cache.files(CONFIGS, it.archive) }
        val bar = progress.begin("Indexing gameval references", work.values.sumOf { it.size }.toLong())
        var indexed = 0
        var failed = 0

        for ((kind, files) in work) {
            for (file in files) {
                bar.step()
                val bytes = cache.data(CONFIGS, kind.archive, file) ?: continue
                val definition = runCatching { GameValReferences.decode(kind, file, bytes, revision) }.getOrNull()
                if (definition == null) {
                    failed++
                    continue
                }
                val refs = GameValReferences.extract(kind, definition, paramTypes, names, knownTables)
                if (refs.isEmpty()) continue
                store.saveConfigRefs(ConfigKey(kind.name, file), crc(bytes), refs)
                indexed++
            }
        }
        bar.close()

        store.markConfigRefIndexed()
        logger.debug { "Indexed gameval references in $indexed config(s)${if (failed > 0) ", $failed undecodable" else ""}" }
    }

    /**
     * [cs2Dir] is the CS2 project, whose script headers type the hook arguments on interface components;
     * without it only configs are relinked. [repackedInterfaces] are the interfaces a packer wrote this
     * build, whose new components get indexed.
     */
    fun relink(
        cache: Cache,
        revision: Int,
        incremental: IncrementalBuild,
        cs2Dir: File? = null,
        repackedInterfaces: Set<Int> = emptySet(),
        progress: CacheProgress = DefaultCacheProgress(),
    ) {
        if (revision < MIN_REVISION) return
        val store = incremental.store ?: return
        if (!store.hasConfigRefIndex()) return

        val all = store.loadConfigRefs()
        val (interfaceRecords, stored) = all.entries.partition { it.key.kind == InterfaceReferences.KIND }
            .let { (a, b) -> a.associate { it.toPair() } to b.associate { it.toPair() } }

        InterfaceReferences.relink(cache, revision, store, cs2Dir, repackedInterfaces, progress, interfaceRecords)
        val fresh = incremental.writtenTargets()
            .asSequence()
            .filter { it.index == CONFIGS }
            .mapNotNull { target -> ConfigRefKind.forArchive(target.archive)?.let { ConfigKey(it.name, target.file) } }
            .filterNot { it in stored }
            .toSet()
        if (stored.isEmpty() && fresh.isEmpty()) return

        val paramTypes by lazy { GameValReferences.loadParamTypes(cache, revision) }
        val names by lazy { GameValReferences.namesFromProvider() }
        val knownTables by lazy { GameValReferences.knownTables(names) }

        val removed = ArrayList<ConfigKey>()
        var reindexed = 0
        var relinked = 0
        val relinkedNames = LinkedHashSet<String>()

        fun reindex(key: ConfigKey, kind: ConfigRefKind, bytes: ByteArray) {
            val definition = runCatching { GameValReferences.decode(kind, key.id, bytes, revision) }.getOrNull() ?: return
            val refs = GameValReferences.extract(kind, definition, paramTypes, names, knownTables)
            if (refs.isEmpty()) removed += key else store.saveConfigRefs(key, crc(bytes), refs)
            reindexed++
        }

        for ((key, record) in stored.asSequence().map { it.toPair() } + fresh.asSequence().map { it to null }) {
            val kind = ConfigRefKind.forName(key.kind) ?: continue
            val bytes = cache.data(CONFIGS, kind.archive, key.id)
            if (bytes == null) {
                if (record != null) removed += key
                continue
            }
            if (record == null || crc(bytes) != record.crc) {
                reindex(key, kind, bytes)
                continue
            }
            relinkOne(cache, key, kind, record, bytes, paramTypes, revision) { encoded, refs ->
                store.saveConfigRefs(key, crc(encoded), refs)
            }?.let { changed ->
                relinked++
                relinkedNames += changed
            }
        }

        store.deleteConfigRefs(removed)

        if (relinked > 0) {
            val sample = relinkedNames.take(SAMPLE).joinToString()
            val more = if (relinkedNames.size > SAMPLE) ", +${relinkedNames.size - SAMPLE} more" else ""
            logger.debug { "Relinked $relinked config(s) to renumbered gamevals: $sample$more" }
        }
        logger.debug { "Gameval reference index: $reindexed re-read, ${removed.size} dropped, $relinked relinked" }
    }

    /** Rewrites [key] when a reference moved; returns the renumbered names, or null when none did. */
    private fun relinkOne(
        cache: Cache,
        key: ConfigKey,
        kind: ConfigRefKind,
        record: StoredConfigRefs,
        bytes: ByteArray,
        paramTypes: Map<Int, ParamType>,
        revision: Int,
        save: (ByteArray, List<ConfigRef>) -> Unit,
    ): List<String>? {
        val moved = record.refs.filter { ref ->
            val current = GameValReferences.currentId(ref.table, ref.name)
            current != null && current != ref.id
        }
        if (moved.isEmpty()) return null

        val definition = runCatching { GameValReferences.decode(kind, key.id, bytes, revision) }
            .getOrElse { failure ->
                logger.warn(failure) { "Could not decode ${key.kind} ${key.id} to relink its gamevals" }
                return null
            }
        val updated = GameValReferences.relink(kind, definition, record.refs, paramTypes, GameValReferences::currentId)
            ?: return null

        val encoded = GameValReferences.encode(kind, definition, revision)
        cache.write(CONFIGS, kind.archive, key.id, encoded)
        save(encoded, updated)
        return moved.map { "${it.table}.${it.name}" }
    }

    private fun crc(bytes: ByteArray): Int = CRC32().apply { update(bytes) }.value.toInt()
}
