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
    private val STRING_SLOT = Regex("""(?<!\\)<(str[0-9]+)""")

    fun apply(root: File) {
        val scripts = File(root, "script")
            .listFiles { file -> file.isFile && file.extension == "cs2" }
            .orEmpty()
        if (scripts.isEmpty()) return

        // Escaping writes the scripts, so it has to produce the text the later fixups work from:
        // retypeProcs copies a script's source to a renamed file and would otherwise write back the
        // unescaped version it was handed.
        val sources = escapeStringSlots(scripts.associateWith { it.readText() })
        retypeProcs(root, sources)
        // Before the dotted pass, which re-reads the prelude and so picks up anything added here.
        declareRenamedCommands(root, File(root, ".prelude"))
        declareDottedCommands(File(root, ".prelude"), sources.values)
    }

    /**
     * Declares the names the dumped scripts use for a command the generated prelude knows under a
     * different one, from [ZwyzCommandTable.DUMP_ALIASES].
     *
     * The prelude and `commands.sym` are generated when the bundle is built, so a command renamed
     * after that — `seq_prefetch` became `preload_keyframeset` in 242 — leaves every call in the
     * dump unresolvable. Declaring the alias here repairs an already extracted project, so a bundle
     * built before the rename does not have to be rebuilt and re-uploaded. Both names share the
     * opcode and signature, so the emitted bytecode is the same either way.
     *
     * Idempotent: an alias that is already declared is skipped, which also means a bundle new enough
     * to carry it from [ZwyzCommandTable] is left alone.
     */
    private fun declareRenamedCommands(root: File, prelude: File) {
        val files = prelude
            .listFiles { file -> file.isFile && file.extension == "cs2" }
            .orEmpty()
        if (files.isEmpty()) return

        val declarations = files.associateWith { it.readLines() }
        val declared = declarations.values.flatMapTo(HashSet()) { lines ->
            lines.mapNotNull { COMMAND_DECL.find(it)?.groupValues?.get(1) }
        }

        val added = mutableListOf<String>()
        for ((target, aliases) in ZwyzCommandTable.DUMP_ALIASES) {
            if (target !in declared) continue
            for (alias in aliases) {
                if (alias in declared) continue
                val entry = declarations.entries.firstNotNullOfOrNull { (file, lines) ->
                    lines.firstOrNull { COMMAND_DECL.find(it)?.groupValues?.get(1) == target }?.let { file to it }
                } ?: continue
                val (file, declaration) = entry
                file.appendText("\n" + declaration.replaceFirst("[command,$target]", "[command,$alias]") + "\n")
                aliasCommandOpcode(File(root, "symbols/commands.sym"), target, alias)
                added += alias
            }
        }

        if (added.isEmpty()) return
        logger.info { "Cs2DumpFixups: declared ${added.size} renamed command(s): ${added.joinToString()}" }
    }

    /** Gives [alias] the opcode [target] already has, so the writer emits the same command. */
    private fun aliasCommandOpcode(symbols: File, target: String, alias: String) {
        if (!symbols.isFile) return
        val lines = symbols.readLines()
        if (lines.any { it.substringAfter('\t', "") == alias }) return
        val opcode = lines.firstOrNull { it.substringAfter('\t', "") == target }?.substringBefore('\t') ?: return
        val separator = if (symbols.readText().endsWith("\n")) "" else "\n"
        symbols.appendText("$separator$opcode\t$alias\n")
    }

    /**
     * Escapes the literal `<` of the client's numbered string substitution slots, `<str0=...>` and
     * `<str0>`.
     *
     * Neptune's `Tag` fragment knows `str` but not the numbered variants, and `STRING_TAG`'s value
     * part is `~('<' | '>')+` so it cannot hold an interpolation either way. From 242 — where
     * `stringTemplates` is on and `bond_main_init` started using `"<str0=<$string1>>"` — the `<`
     * falls through to `STRING_EXPR_START` and the parse fails on the `=`. `\<` is the grammar's
     * own literal-`<` escape and compiles to a bare `<`, so the emitted string is byte for byte
     * what it was.
     *
     * Idempotent: the lookbehind skips a slot that has already been escaped, which matters because
     * this runs on every install.
     */
    private fun escapeStringSlots(sources: Map<File, String>): Map<File, String> {
        var patched = 0
        val escaped = sources.mapValues { (file, text) ->
            val updated = STRING_SLOT.replace(text) { """\<""" + it.groupValues[1] }
            if (updated == text) return@mapValues text
            file.writeText(updated)
            patched++
            updated
        }
        if (patched > 0) {
            logger.info { "Cs2DumpFixups: escaped numbered string slots in $patched script(s)" }
        }
        return escaped
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
