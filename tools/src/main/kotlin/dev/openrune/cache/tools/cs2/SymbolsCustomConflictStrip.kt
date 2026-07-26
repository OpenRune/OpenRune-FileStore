package dev.openrune.cache.tools.cs2

import com.github.michaelbull.logging.InlineLogger
import java.io.File

/**
 * Neptune loads both `symbols/` and `symbols_custom/`. SymDumper writes cache gamevals into
 * `symbols/`, which collides with plugin entries staged under `symbols_custom/` (same id and/or
 * name). Strip those overlapping lines from the base symbol files so only the custom copy remains.
 */
object SymbolsCustomConflictStrip {
    private val logger = InlineLogger()

    /** id (may be composite like `55521:0`) + symbol name (second TSV field). */
    private val SYMBOL_LINE = Regex("""^\s*(\S+)\s+(\S+)(?:\s+.*)?$""")

    fun strip(cs2Dir: File) {
        val symbolsDir = File(cs2Dir, "symbols")
        val customDir = File(cs2Dir, "symbols_custom")
        if (!symbolsDir.isDirectory || !customDir.isDirectory) {
            return
        }

        customDir.listFiles()?.filter { it.isFile && it.extension.equals("sym", true) }?.forEach { customSym ->
            val owned = readEntries(customSym)
            if (owned.isEmpty()) {
                return@forEach
            }
            val baseSym = File(symbolsDir, customSym.name)
            val removed = stripOverlaps(baseSym, owned)
            if (removed > 0) {
                logger.info {
                    "Stripped $removed symbol(s) from symbols/${customSym.name} that overlap symbols_custom"
                }
            }
        }
    }

    private data class Entry(val id: String, val name: String)

    private fun readEntries(file: File): List<Entry> =
        file.readLines().mapNotNull { line ->
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                return@mapNotNull null
            }
            val match = SYMBOL_LINE.matchEntire(trimmed) ?: return@mapNotNull null
            Entry(match.groupValues[1], match.groupValues[2])
        }

    private fun stripOverlaps(target: File, owned: List<Entry>): Int {
        if (!target.exists() || owned.isEmpty()) {
            return 0
        }
        val ownedIds = owned.map { it.id }.toSet()
        val ownedNames = owned.map { it.name }.toSet()
        val original = target.readLines()
        val kept =
            original.filter { line ->
                val trimmed = line.trim()
                if (trimmed.isEmpty()) {
                    return@filter false
                }
                val match = SYMBOL_LINE.matchEntire(trimmed) ?: return@filter true
                val id = match.groupValues[1]
                val name = match.groupValues[2]
                id !in ownedIds && name !in ownedNames
            }
        val removed = original.size - kept.size
        if (removed <= 0) {
            return 0
        }
        if (kept.isEmpty()) {
            target.delete()
        } else {
            target.writeText(kept.joinToString("\n", postfix = "\n"))
        }
        return removed
    }
}
