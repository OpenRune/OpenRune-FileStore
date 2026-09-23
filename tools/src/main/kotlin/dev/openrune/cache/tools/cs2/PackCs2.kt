package dev.openrune.cache.tools.cs2

import com.github.michaelbull.logging.InlineLogger
import dev.openrune.cache.CLIENTSCRIPT
import dev.openrune.cache.tools.TaskPriority
import dev.openrune.cache.tools.progress.CacheProgress
import dev.openrune.cache.tools.tasks.CacheTask
import dev.openrune.cache.util.progress
import dev.openrune.clientscript.compiler.ClientScripts
import dev.openrune.clientscript.compiler.ScriptEntry
import dev.openrune.definition.constants.ConstantProvider
import dev.openrune.filesystem.Cache
import java.io.File
import kotlin.io.path.Path

/**
 * Compiles Neptune CS2 sources and writes client scripts into the cache.
 *
 * Automatically installs or refreshes the bundled default CS2 project when:
 * - this is the first run,
 * - neptune.toml is missing,
 * - client_version differs from the cache revision.
 *
 * Directories listed in neptune.toml that are missing are simply recreated.
 *
 * Runs at [TaskPriority.CS2], the last stage: the symbol dump reads gamevals from the cache, so every
 * packer and [dev.openrune.cache.tools.tasks.impl.PackGameVals] must already have run.
 */

class PackCs2(private val cs2Dir: File, private val overrides: Cs2Overrides = Cs2Overrides()) : CacheTask() {

    private val logger = InlineLogger()

    override val priority: TaskPriority
        get() = TaskPriority.CS2

    companion object {
        private val SCRIPT_ARCHIVE = Regex("""\[(\w+),([^\]]+)]""")

        private val logger = InlineLogger()

        /** Neptune's per-script record of the symbol ids each library script was last compiled with. */
        const val LIBRARY_STATE_FILE = ".library-symbols"

        /**
         * Write every library script on every build instead of only the ones whose symbol ids moved. On
         * for now: with custom gamevals overriding the vanilla ids this guarantees the cache's scripts
         * always carry the current ids, at the cost of writing the whole library each build. Turning it
         * off returns to Neptune's baseline tracking.
         */
        var rewriteAllLibraries: Boolean = true

        /** Below this a payload cannot be a compiled script; see [PackCs2.selectWritable]. */
        private const val MIN_SCRIPT_BYTES = 2

        /** Symbol tables dumped from the cache before this build changed anything; consumed by [recordLibraryBaseline]. */
        private const val BASELINE_SNAPSHOT_DIR = "symbols_snapshot"

        /** The merged tables the baseline compile reads; built and removed by [recordLibraryBaseline]. */
        private const val BASELINE_SYMBOLS_DIR = "symbols_baseline"

        /**
         * First half of establishing Neptune's library baseline: dump the symbol tables while the cache still
         * holds the ids its scripts were compiled with. Runs before any packer, only when the project is
         * installed and has no baseline yet. Neptune only writes the baseline when it compiles, and the
         * first compile can easily land in a build that has already renumbered gamevals; a baseline taken
         * against those ids would hide every rename that happened before it.
         */
        fun snapshotBaselineSymbols(cs2Dir: File, cache: Cache, revision: Int, progress: CacheProgress) {
            if (rewriteAllLibraries) return
            if (!File(cs2Dir, "neptune.toml").isFile || File(cs2Dir, LIBRARY_STATE_FILE).isFile) return
            if (revision < CacheTask.CS2_MIN_CACHE_REVISION) return
            val snapshot = File(cs2Dir, BASELINE_SNAPSHOT_DIR)
            snapshot.deleteRecursively()
            snapshot.mkdirs()
            SymDumper.dumpCacheVals(snapshot, cache, revision, progress)
        }

        /**
         * id, name and the optional trailing columns (a varp's type, say). Fields are tab-separated and a
         * name may contain spaces (`burgh_map:temple hi model 1`), so this must not split on whitespace.
         */
        private val SYMBOL_LINE = Regex("""^([^\t]+)\t([^\t]+)(?:\t(.*))?$""")

        /**
         * [current] as it stands after this build, with every name that also appears in [snapshot] taking
         * the snapshot's (pre-build) id. Names the build introduced (a new interface's components, say)
         * keep their current line, and a current line whose id the snapshot already uses under another name
         * is dropped so Neptune sees no duplicate ids.
         *
         * The snapshot is a bare cache dump, so it lacks the columns the bundled tables carry (the varp
         * type column). A snapshot line with no trailing columns borrows them from the current line of the
         * same name; without that every `%varp` in the baseline compile types as `unit` and it fails.
         */
        internal fun mergeBaselineSymbols(current: List<String>, snapshot: List<String>): List<String> {
            val currentExtraByName = HashMap<String, String>()
            current.forEach { line ->
                val match = SYMBOL_LINE.matchEntire(line.trim()) ?: return@forEach
                val extra = match.groupValues[3].trim()
                if (extra.isNotEmpty()) currentExtraByName.putIfAbsent(match.groupValues[2], extra)
            }
            val snapshotNames = HashSet<String>()
            val snapshotIds = HashSet<String>()
            val merged = snapshot.map { line ->
                val match = SYMBOL_LINE.matchEntire(line.trim()) ?: return@map line
                val (id, name, extra) = match.destructured
                snapshotIds += id
                snapshotNames += name
                val borrowed = if (extra.isBlank()) currentExtraByName[name] else null
                if (borrowed != null) "$id\t$name\t$borrowed" else line
            }
            val kept = current.filter { line ->
                val match = SYMBOL_LINE.matchEntire(line.trim()) ?: return@filter true
                match.groupValues[2] !in snapshotNames && match.groupValues[1] !in snapshotIds
            }
            return merged + kept
        }
    }

    /**
     * Second half: with the whole project staged (pack sources, custom symbols, this build's symbol dump),
     * compile it once against the current tables merged with the pre-build snapshot so Neptune records the
     * ids the cache's scripts were compiled with. The normal compile that follows then sees exactly which
     * library scripts moved. Costs one extra compile the first time.
     */
    private fun recordLibraryBaseline(configFile: File) {
        val snapshot = File(cs2Dir, BASELINE_SNAPSHOT_DIR)
        if (rewriteAllLibraries) {
            snapshot.deleteRecursively()
            return
        }
        if (!snapshot.isDirectory) return
        val baseline = File(cs2Dir, BASELINE_SYMBOLS_DIR)
        val baselineConfig = File(cs2Dir, "neptune.baseline.toml")
        try {
            if (File(cs2Dir, LIBRARY_STATE_FILE).isFile) return
            logger.info { "Recording CS2 library baseline against the pre-build symbol tables (first compile of this project)" }

            baseline.deleteRecursively()
            val symbols = File(cs2Dir, "symbols")
            symbols.copyRecursively(baseline, overwrite = true)
            snapshot.listFiles { f -> f.isFile && f.extension == "sym" }?.forEach { snapFile ->
                val target = File(baseline, snapFile.name)
                val current = if (target.isFile) target.readLines() else emptyList()
                target.writeText(mergeBaselineSymbols(current, snapFile.readLines()).joinToString("\n", postfix = "\n"))
            }
            SymbolsCustomConflictStrip.strip(cs2Dir, BASELINE_SYMBOLS_DIR)

            baselineConfig.writeText(
                NeptuneProjectManifest.configWithSymbols(configFile, listOf("$BASELINE_SYMBOLS_DIR/", "${NeptuneProjectManifest.CUSTOM_SYMBOLS}/")),
            )
            ClientScripts.compileTask(baselineConfig.toPath(), revision)
        } catch (e: Exception) {
            logger.warn(e) { "Could not record the CS2 library baseline; library scripts will not be repacked for renumbered gamevals until a compile succeeds" }
        } finally {
            baselineConfig.delete()
            baseline.deleteRecursively()
            snapshot.deleteRecursively()
        }
    }
    val cs2Root: File
        get() = cs2Dir

    override fun init(cache: Cache) {
        try {
            if (revision < 0) {
                logger.warn {
                    "PackCs2: cache revision is unset (-1). BuildCache must assign revision before CS2 tasks run."
                }
                return
            }

            if (revision < CacheTask.CS2_MIN_CACHE_REVISION) {
                logger.warn {
                    "PackCs2: cache revision $revision is not supported (minimum ${CacheTask.CS2_MIN_CACHE_REVISION})."
                }
                return
            }

            ensureInstalled(cache)

            val configFile = File(cs2Dir, "neptune.toml")

            if (!validateNeptuneLayout(configFile)) {
                return
            }

            NeptuneTomlClientVersion.patch(configFile, revision)

            SymDumper.dumpCacheVals(File(cs2Dir, "symbols"), cache, revision, progress)

            // Everything the project contributes is written into neptune.toml and symbols_custom/
            // rather than passed to the compiler directly, so an IDE reading the same config sees
            // exactly what the build does.
            NeptuneProjectManifest.update(configFile, cs2Dir, overrides)

            // SymDumper may re-emit plugin gamevals into symbols/; drop overlaps so
            // symbols_custom wins (Neptune rejects duplicate ids/names).
            SymbolsCustomConflictStrip.strip(cs2Dir)

            CustomCs2OverrideSync(cs2Dir, revision, overrides.sources).sync()

            recordLibraryBaseline(configFile)

            // Always compiled, never skipped by the incremental engine: Neptune decides for itself which
            // library scripts need writing (see LibrarySymbolState), and a project-level fingerprint skip
            // sat in front of that and left the cache stale.
            compileAndWrite(cache, configFile)
        } catch (e: Exception) {
            logger.error(e) {
                "PackCs2 failed"
            }
        }
    }

    private fun compileAndWrite(cache: Cache, configFile: File) {
        // Neptune's own progress is kept out of the log, and a full compile is the longest quiet
        // stretch of a build, so say what is happening before it starts.
        logger.info { if (rewriteAllLibraries) "Compiling CS2 scripts (all library scripts will be written)" else "Compiling CS2 scripts" }
        val scripts = ClientScripts.compileTask(configFile.toPath(), revision, rewriteAllLibraries = rewriteAllLibraries)
        val changedLibraries = scripts.count { it.library }

        // With baseline tracking, a library script is only written when a symbol it uses was renumbered;
        // name each one so a gameval change can be traced to its scripts. Pointless when all are written.
        if (!rewriteAllLibraries) {
            scripts.filter { it.library }.forEach { script ->
                logger.debug { "Repacked script ${script.archiveName} (${resolveScriptId(script)}): a symbol it uses was renumbered" }
            }
        }

        val writable = selectWritable(scripts)

        val label = if (rewriteAllLibraries) "Packing Cs2 Scripts ($changedLibraries library scripts)" else "Packing Cs2 Scripts ($changedLibraries changed library scripts)"
        val bar = this.progress.begin(label, writable.size)

        writable.forEach { (id, script) ->
            cache.write(CLIENTSCRIPT, id, script.bytes)
            bar.step()
        }

        bar.close()
    }

    /**
     * One entry per script id, dropping payloads too short to be a script.
     *
     * A compiled script always carries its name and trailer, so anything this short is the compiler
     * handing back an entry it did not actually produce bytes for. Writing it replaces a working
     * script in the cache with a stub, which only shows up as a broken client, so those are skipped
     * and the longest payload wins when one id is emitted more than once.
     */
    private fun selectWritable(scripts: List<ScriptEntry>): Map<Int, ScriptEntry> {
        val byId = LinkedHashMap<Int, ScriptEntry>()
        val skipped = ArrayList<String>()
        for (script in scripts) {
            if (script.bytes.size <= MIN_SCRIPT_BYTES) {
                skipped += "${script.archiveName} (${script.bytes.size} bytes)"
                continue
            }
            val id = resolveScriptId(script)
            val existing = byId[id]
            if (existing != null && existing.bytes.size >= script.bytes.size) continue
            byId[id] = script
        }
        if (skipped.isNotEmpty()) {
            logger.warn {
                "Skipped ${skipped.size} empty script(s) rather than blanking what the cache already has: " +
                    skipped.take(10).joinToString() + if (skipped.size > 10) ", +${skipped.size - 10} more" else ""
            }
        }
        return byId
    }

    /**
     * Prefer RSCM/gameval id. Tries full archive key (`clientscript.[proc,foo]`) then
     * short toml key (`clientscript.foo`). Falls back to Neptune `.sym` id.
     */
    private fun resolveScriptId(script: ScriptEntry): Int {
        ConstantProvider.getMappingOrNull("clientscript.${script.archiveName}")?.let { return it }

        val match = SCRIPT_ARCHIVE.matchEntire(script.archiveName) ?: return script.id
        val name = match.groupValues[2]
        ConstantProvider.getMappingOrNull("clientscript.$name")?.let { return it }

        return script.id
    }

    private fun ensureInstalled(cache: Cache) {
        val neptune = File(cs2Dir, "neptune.toml")

        var needsInstall = !neptune.exists()

        if (!needsInstall) {
            val text = runCatching {
                neptune.readText()
            }.getOrNull()

            if (text == null) {
                needsInstall = true
            } else {
                // Missing directories are recreated by validateNeptuneLayout; only a missing config or a
                // different client version means the bundled project has to be installed again.
                val existingVersion =
                    NeptuneTomlClientVersion
                        .readClientVersionFromText(text)

                needsInstall = existingVersion != revision
            }
        }

        if (!needsInstall) {
            return
        }

        logger.debug {
            "PackCs2: CS2 project missing or outdated, unpacking bundled defaults."
        }

        // Installed this late in the build, the cache no longer carries the original ids, so no baseline
        // can be taken here; the next build records it before its packers run.
        val unpack = UnpackDefaultCs2(
            cs2Directory = cs2Dir,
            force = true,
            recordBaseline = false,
        )

        unpack.revision = revision
        unpack.subRevision = subRevision
        unpack.progress = progress
        unpack.init(cache)
    }

    private fun validateNeptuneLayout(
        configFile: File
    ): Boolean {
        if (!configFile.exists()) {
            logger.warn {
                "PackCs2: neptune.toml does not exist after installation."
            }
            return false
        }

        val text = runCatching {
            configFile.readText()
        }.getOrElse {
            logger.warn {
                "PackCs2: could not read neptune.toml: ${it.message}"
            }
            return false
        }

        NeptuneTomlClientVersion.ensureExcludedDirectories(
            cs2Dir,
            text
        )

        for (key in NeptuneTomlClientVersion.neptuneDirectoryArrayKeys) {
            for (rel in parseNeptuneStringArray(text, key)) {
                // pack paths are absolute and may be single files; they are the packs' to provide
                if (File(rel).isAbsolute) {
                    continue
                }
                val dir = NeptuneTomlClientVersion.resolveEntry(cs2Dir, rel)

                if (dir.exists()) {
                    continue
                }

                if (!dir.mkdirs()) {
                    logger.warn {
                        "PackCs2: could not create $key directory ${dir.absolutePath}."
                    }
                    return false
                }

                logger.debug {
                    "PackCs2: created missing $key directory ${dir.absolutePath}"
                }
            }
        }

        return true
    }

    private fun parseNeptuneStringArray(
        text: String,
        key: String
    ): List<String> {
        val match = Regex(
                """(?m)^\s*$key\s*=\s*\[(.*?)]\s*(?:#.*)?$"""
            ).find(text)
                ?: return emptyList()

        return match.groupValues[1]
            .split(',')
            .map {
                it.trim()
                    .removeSurrounding("\"")
                    .removeSurrounding("'")
                    .trim()
            }
            .filter {
                it.isNotEmpty()
            }
    }
}