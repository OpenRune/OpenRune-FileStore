package dev.openrune.cache.tools.cs2

import com.github.michaelbull.logging.InlineLogger
import dev.openrune.cache.CLIENTSCRIPT
import dev.openrune.cache.tools.TaskPriority
import dev.openrune.cache.tools.incremental.Hashing
import dev.openrune.cache.tools.tasks.CacheTask
import dev.openrune.cache.util.progress
import dev.openrune.clientscript.compiler.ClientScripts
import dev.openrune.clientscript.compiler.ScriptEntry
import dev.openrune.definition.constants.ConstantProvider
import dev.openrune.filesystem.Cache
import java.io.File
import java.security.MessageDigest
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

    private companion object {
        private val SCRIPT_ARCHIVE = Regex("""\[(\w+),([^\]]+)]""")
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

            // Neptune compiles the project as a whole and reports no per-script dependencies, so CS2 is one
            // coarse unit: any source, symbol or library file added, edited or removed recompiles the lot,
            // and an untouched project skips compilation entirely. The fingerprint is taken after the
            // symbol dump above, so a config or gameval change that alters a symbol also triggers a rebuild.
            incremental.runOnce(
                task = this,
                scope = cs2Dir.absolutePath,
                label = "Packing Cs2 Scripts",
                cache = cache,
                fingerprint = fingerprintProject(configFile),
            ) { packCache ->
                compileAndWrite(packCache, configFile)
            }
        } catch (e: Exception) {
            logger.error(e) {
                "PackCs2 failed"
            }
        }
    }

    private fun compileAndWrite(cache: Cache, configFile: File) {
        // Neptune's own progress is kept out of the log, and a full compile is the longest quiet
        // stretch of a build, so say what is happening before it starts.
        logger.info { "Compiling CS2 scripts" }
        val scripts = ClientScripts.compileTask(configFile.toPath(), revision)
        val changedLibraries = scripts.count { it.library }

        val bar = this.progress.begin("Packing Cs2 Scripts ($changedLibraries changed library scripts)", scripts.size)

        scripts.forEach { script ->
            val id = resolveScriptId(script)
            cache.write(CLIENTSCRIPT, id, script.bytes)
            bar.step()
        }

        bar.close()
    }

    /**
     * Fingerprints every file Neptune reads: `neptune.toml` plus the trees named by its `sources`, `symbols`
     * and `libraries` keys. Generated output under `excluded` is skipped so a rebuild does not appear to
     * change its own inputs.
     *
     * The fingerprint is over each file's path, size and modification time rather than its contents. A
     * project is close to ten thousand files, and reading each one took a minute on Windows where a stat
     * is a fraction of a millisecond; an edit that keeps both size and mtime identical is not something a
     * save produces. `script/` is normally listed under both `sources` and `libraries`, so roots are
     * de-duplicated before walking.
     */
    private fun fingerprintProject(configFile: File): String {
        val text = runCatching { configFile.readText() }.getOrNull()
            ?: return Hashing.hashTree(cs2Dir)

        val excluded = parseNeptuneStringArray(text, "excluded")
            .map { NeptuneTomlClientVersion.resolveEntry(cs2Dir, it).absoluteFile }

        val roots = listOf("sources", "symbols", "libraries")
            .flatMap { key -> parseNeptuneStringArray(text, key) }
            .map { NeptuneTomlClientVersion.resolveEntry(cs2Dir, it).absoluteFile }
            .distinct()
            .filter { it.exists() }

        val files = roots.asSequence()
            .flatMap { it.walkTopDown() }
            .filter { it.isFile }
            .filterNot { file -> excluded.any { file.absoluteFile.startsWith(it) } }
            .plus(configFile.absoluteFile)
            .distinctBy { it.absolutePath }
            .sortedBy { it.absolutePath }

        val digest = MessageDigest.getInstance("SHA-256")
        for (file in files) {
            digest.update(file.absolutePath.replace('\\', '/').toByteArray())
            digest.update(file.length().toString().toByteArray())
            digest.update(file.lastModified().toString().toByteArray())
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
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

        val unpack = UnpackDefaultCs2(
            cs2Directory = cs2Dir,
            force = true
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