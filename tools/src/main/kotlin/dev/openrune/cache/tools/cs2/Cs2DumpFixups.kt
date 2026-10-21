package dev.openrune.cache.tools.cs2

import com.github.michaelbull.logging.InlineLogger
import java.io.File

/**
 * Repairs the inconsistencies a freshly unpacked bundle can carry, in place.
 *
 * Both come from the dump and its command tables being generated independently:
 * - A script only ever reached through `~name` can arrive declared `[clientscript,name]`, because the
 *   decompiler takes the name from `clientscript.sym` and the trigger from the cache. Neptune then
 *   resolves neither the declaration nor its callers, so the script is retyped to `[proc,name]` and
 *   `clientscript.sym` follows, keeping the name the callers use.
 * - A `.command` call can have no dotted declaration in the prelude, which only carries the variants
 *   [ZwyzCommandTable.hasSecondary] knows about. The bare declaration is copied with the dot added;
 *   both forms share one opcode, so the signature is unchanged.
 *
 * Applied after extraction rather than when bundles are built: a bundle is built from whatever dump
 * was current then and is not rebuilt when a later dump corrects itself.
 */
internal object Cs2DumpFixups {

    private val logger = InlineLogger()

    private val CLIENTSCRIPT_FILE = Regex("""^\[clientscript,(.+)]\.cs2$""")
    private val PROC_CALL = Regex("""~([A-Za-z0-9_]+)""")
    private val QUOTED_NAME = Regex(""""([A-Za-z0-9_]+)""")
    private val DOTTED_CALL = Regex("""(?<![A-Za-z0-9_${'$'}%^.])\.([a-z][a-z0-9_]*)""")
    private val COMMAND_DECL = Regex("""^\[command,([^]]+)]""")

    fun apply(root: File) {
        val scripts = File(root, "script")
            .listFiles { file -> file.isFile && file.extension == "cs2" }
            .orEmpty()
        if (scripts.isEmpty()) return

        val sources = scripts.associateWith { it.readText() }
        retypeProcs(root, sources)
        declareDottedCommands(File(root, ".prelude"), sources.values)
    }

    private fun retypeProcs(root: File, sources: Map<File, String>) {
        val procCalls = sources.values.flatMapTo(HashSet()) { text ->
            PROC_CALL.findAll(text).map { it.groupValues[1] }
        }
        // A name used in a string is a clientscript reference (a hook, say) and has to stay one.
        val quoted = sources.values.flatMapTo(HashSet()) { text ->
            QUOTED_NAME.findAll(text).map { it.groupValues[1] }
        }
        val fileNames = sources.keys.mapTo(HashSet()) { it.name }

        val retyped = mutableListOf<String>()
        for ((file, text) in sources) {
            val name = CLIENTSCRIPT_FILE.matchEntire(file.name)?.groupValues?.get(1) ?: continue
            if (name !in procCalls || name in quoted || "[proc,$name].cs2" in fileNames) continue

            val target = File(file.parentFile, "[proc,$name].cs2")
            target.writeText(text.replaceFirst("[clientscript,$name]", "[proc,$name]"))
            file.delete()
            retyped += name
        }

        if (retyped.isEmpty()) return

        retypeSymbols(File(root, "symbols/clientscript.sym"), retyped)
        logger.info { "Cs2DumpFixups: retyped ${retyped.size} clientscript(s) called as procs: ${retyped.joinToString()}" }
    }

    private fun retypeSymbols(symbols: File, retyped: List<String>) {
        if (!symbols.isFile) return
        val replacements = retyped.associate { "[clientscript,$it]" to "[proc,$it]" }
        var changed = false
        val lines = symbols.readLines().map { line ->
            val name = line.substringAfter('\t', "")
            val replacement = replacements[name] ?: return@map line
            changed = true
            line.substringBefore('\t') + "\t" + replacement
        }
        if (changed) symbols.writeText(lines.joinToString("\n", postfix = "\n"))
    }

    private fun declareDottedCommands(prelude: File, sources: Collection<String>) {
        val files = prelude
            .listFiles { file -> file.isFile && file.extension == "cs2" }
            .orEmpty()
        if (files.isEmpty()) return

        val declarations = files.associateWith { it.readLines() }
        val declared = declarations.values.flatMapTo(HashSet()) { lines ->
            lines.mapNotNull { COMMAND_DECL.find(it)?.groupValues?.get(1) }
        }
        val wanted = sources
            .flatMapTo(HashSet()) { text -> DOTTED_CALL.findAll(text).map { it.groupValues[1] } }
            .filterTo(HashSet()) { it in declared && ".$it" !in declared }
        if (wanted.isEmpty()) return

        for ((file, lines) in declarations) {
            val added = lines.mapNotNull { line ->
                val name = COMMAND_DECL.find(line)?.groupValues?.get(1) ?: return@mapNotNull null
                if (name in wanted) line.replaceFirst("[command,", "[command,.") else null
            }
            if (added.isEmpty()) continue
            file.appendText(added.joinToString("\n", prefix = "\n", postfix = "\n"))
        }

        logger.info { "Cs2DumpFixups: declared ${wanted.size} dotted command variant(s): ${wanted.joinToString { ".$it" }}" }
    }
}
