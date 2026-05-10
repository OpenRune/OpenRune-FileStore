package dev.openrune.cache.tools.cs2

import com.github.michaelbull.logging.InlineLogger
import java.io.File

/**
 * Keeps `client_version` in neptune.toml aligned with the cache revision used by [PackCs2] / [UnpackDefaultCs2].
 *
 * [ensureNeptuneToml] creates `neptune.toml` from `/packcs2/neptune.default.toml` when missing (e.g. after unpack).
 */
internal object NeptuneTomlClientVersion {

    /** TOML array keys whose entries are directory paths under the CS2 root (same as [PackCs2] layout checks). */
    internal val neptuneDirectoryArrayKeys = listOf("sources", "symbols", "libraries", "excluded")

    private val logger = InlineLogger()

    private const val DEFAULT_NEPTUNE_RESOURCE = "/packcs2/neptune.default.toml"

    private val clientVersionLine =
        Regex("""^(\s*)client_version(\s*=\s*)\d+(\s*)$""", RegexOption.MULTILINE)

    private val clientVersionValueLine =
        Regex("""(?m)^\s*client_version\s*=\s*(\d+)\s*(?:#.*)?$""")

    private val templateClientVersionLine =
        Regex("""^client_version\s*=\s*\d+\s*$""", RegexOption.MULTILINE)

    /** First `client_version = …` line in TOML text, if present. */
    internal fun readClientVersionFromText(text: String): Int? =
        clientVersionValueLine.find(text)?.groupValues?.get(1)?.toIntOrNull()

    internal fun readClientVersion(configFile: File): Int? {
        if (!configFile.exists()) return null
        val text = runCatching { configFile.readText() }.getOrNull() ?: return null
        return readClientVersionFromText(text)
    }

    /** Every path listed under [neptuneDirectoryArrayKeys] exists as a directory under [cs2Root]. */
    internal fun allNeptunePathDirectoriesExist(cs2Root: File, neptuneText: String): Boolean {
        for (key in neptuneDirectoryArrayKeys) {
            for (rel in parseTomlStringArray(neptuneText, key)) {
                val dir = File(cs2Root, rel.trimEnd('/', ' '))
                if (!dir.isDirectory) return false
            }
        }
        return true
    }

    fun ensureNeptuneToml(configFile: File, rev: Int) {
        if (!configFile.exists()) {
            if (!writeDefaultFromClasspath(configFile, rev)) {
                return
            }
        }
        patch(configFile, rev)
    }

    fun ensureExcludedDirectories(cs2Root: File, neptuneText: String) {
        for (rel in parseTomlStringArray(neptuneText, "excluded")) {
            File(cs2Root, rel.trimEnd('/', ' ')).mkdirs()
        }
    }

    private fun parseTomlStringArray(text: String, key: String): List<String> {
        val m = Regex("""(?m)^\s*$key\s*=\s*\[(.*?)]\s*(?:#.*)?$""").find(text) ?: return emptyList()
        return m.groupValues[1].split(',')
            .map { it.trim().removeSurrounding("\"").removeSurrounding("'").trim() }
            .filter { it.isNotEmpty() }
    }

    private fun writeDefaultFromClasspath(configFile: File, rev: Int): Boolean {
        val stream = NeptuneTomlClientVersion::class.java.getResourceAsStream(DEFAULT_NEPTUNE_RESOURCE)
        if (stream == null) {
            logger.warn {
                "NeptuneToml: classpath resource $DEFAULT_NEPTUNE_RESOURCE is missing; cannot create neptune.toml " +
                    "at ${configFile.absolutePath}."
            }
            return false
        }
        val text = stream.bufferedReader().use { it.readText() }
        val body = templateClientVersionLine.replace(text) { "client_version = $rev" }
        configFile.parentFile?.mkdirs()
        configFile.writeText(body.trimEnd() + "\n")
        logger.info { "NeptuneToml: created default neptune.toml at ${configFile.absolutePath}" }
        return true
    }

    fun patch(configFile: File, rev: Int): Boolean {
        if (!configFile.exists()) return false
        val text = runCatching { configFile.readText() }.getOrElse { return false }
        val next = if (clientVersionLine.containsMatchIn(text)) {
            clientVersionLine.replace(text) { m ->
                "${m.groupValues[1]}client_version${m.groupValues[2]}$rev${m.groupValues[3]}"
            }
        } else {
            val tail = if (text.isNotEmpty() && !text.endsWith('\n')) "\n" else ""
            text + tail + "client_version = $rev\n"
        }
        if (next != text) {
            configFile.writeText(next)
        }
        return true
    }
}
