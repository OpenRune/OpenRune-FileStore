package dev.openrune.cache.tools.cs2

import com.github.michaelbull.logging.InlineLogger
import java.io.File

/**
 * Keeps `neptune.toml` describing the whole build: the project's own directories plus every
 * script path the packs contribute, and `symbols_custom/` holding the symbol lines they and the
 * gamevals supply. Rewritten on every run, so a pack added, removed or renamed shows up in the
 * config - and in any IDE reading it - without scripts being copied around.
 */
object NeptuneProjectManifest {
    private val logger = InlineLogger()

    /** Written fresh every run, so its absence never means the project needs reinstalling. */
    internal const val CUSTOM_SYMBOLS = "symbols_custom"
    private val PROJECT_SOURCES = listOf(".prelude/", "script/")
    private val SYMBOL_PATHS = listOf("symbols/", "$CUSTOM_SYMBOLS/")

    fun update(configFile: File, cs2Dir: File, overrides: Cs2Overrides) {
        writeSymbols(File(cs2Dir, CUSTOM_SYMBOLS), overrides.symbols)

        val sources = PROJECT_SOURCES + overrides.sources.map { it.manifestPath(cs2Dir) }
        val symbols = SYMBOL_PATHS

        val text = configFile.readText()
        val updated = text
            .replaceArray("sources", sources)
            .replaceArray("symbols", symbols)
        if (updated != text) {
            configFile.writeText(updated)
            logger.debug { "neptune.toml: ${sources.size} sources, ${symbols.size} symbol dirs" }
        }
    }

    private fun writeSymbols(dir: File, symbols: Map<String, List<String>>) {
        dir.deleteRecursively()
        if (symbols.isEmpty()) {
            return
        }
        dir.mkdirs()
        for ((table, lines) in symbols) {
            File(dir, "$table.sym").writeText(lines.joinToString("\n", postfix = "\n"))
        }
    }

    /** Paths inside the project stay relative so the config keeps working if the directory moves. */
    private fun File.manifestPath(cs2Dir: File): String {
        val absolute = absoluteFile.normalize()
        val root = cs2Dir.absoluteFile.normalize()
        val path = if (absolute.startsWith(root)) absolute.relativeTo(root).path else absolute.path
        val slashed = path.replace('\\', '/')
        return if (isDirectory && !slashed.endsWith("/")) "$slashed/" else slashed
    }

    private fun String.replaceArray(key: String, values: List<String>): String {
        val line = "$key = [" + values.joinToString(", ") { "\"$it\"" } + "]"
        val regex = Regex("""(?m)^\s*$key\s*=\s*\[.*?]\s*(?:#.*)?$""")
        return if (regex.containsMatchIn(this)) regex.replace(this) { Regex.escapeReplacement(line) } else trimEnd() + "\n" + line + "\n"
    }
}
