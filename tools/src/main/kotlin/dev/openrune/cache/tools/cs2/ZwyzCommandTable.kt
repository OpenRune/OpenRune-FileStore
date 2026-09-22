package dev.openrune.cache.tools.cs2

import net.lingala.zip4j.ZipFile
import java.io.File

/**
 * Generates `symbols/commands.sym` and the `.prelude` command signatures for one client
 * revision from the `data/commands` text files in `zwyz/osrs-cache`, the same tables its decompiler uses to
 * produce the scripts in `Joshua-F/osrs-dumps`. Using the same source keeps names, opcodes and
 * signatures in step with the decompiled scripts for every revision.
 *
 * File format (one command per line):
 * ```
 * <opcode> [command,<name>](<type> $<param>, ...)(<return>, ...) [<minRevision>]
 * ```
 * Lines whose trailing revision is above the target are ignored; otherwise later lines override
 * earlier ones by name and by opcode, mirroring `Command.reset()` in the zwyz source. Lines with
 * `(gap)` or a `//` prefix are skipped, and a line with no signature declares an opcode with an
 * unknown signature.
 *
 * Neptune differences handled here:
 * - zwyz's enum-like types (`iftype`, `rgb`, `key`, ...) are plain `int` in Neptune.
 * - `unknown` is `any`, `unknownarray` is `array`.
 * - a `clientscript $x, argument_list $y` pair is one Neptune `hook` (`stathook`, `invhook`,
 *   `varphook` for the transmit variants).
 * - commands Neptune implements with a dynamic handler are declared bare, signature in a comment.
 * - `basevartype` arguments are an implementation detail of those handlers and are dropped.
 * - core opcodes (file `0_core.txt`) are emitted by the compiler itself and get no declaration.
 * - `cc_*` commands and a few `if_*` commands also get a `.name` secondary declaration.
 */
class ZwyzCommandTable private constructor(
    val commitSha: String,
    /** Source file stem (e.g. `100_interface_core`) -> raw lines. */
    private val files: List<Pair<String, List<String>>>,
) {
    data class Command(
        val opcode: Int,
        val name: String,
        /** null when the source line had no signature. */
        val params: List<Pair<String, String>>?,
        val returns: List<String>,
        val sourceFile: String,
    )

    class Resolved internal constructor(
        val revision: Int,
        val commands: List<Command>,
        val commitSha: String,
        /** Opcode -> extra names the same opcode had at another point in zwyz history. */
        val aliases: Map<Int, Set<String>> = emptyMap(),
    ) {
        val byName: Map<String, Command> = commands.associateBy { it.name }

        /**
         * Adds the names [other] gives the same opcodes, so a dump produced by an older or newer
         * decompiler still resolves. Joshua-F generates each revision's dump with whatever decompiler
         * was current then and does not always regenerate later, so a 2025 dump calls `sin_deg` where
         * zwyz master now says `sin`. A name already used by another command is left out.
         */
        fun withAliases(other: Resolved): Resolved {
            val opcodes = commands.associateBy { it.opcode }
            // merge with the aliases already collected so several historical tables can be folded in
            val extra = aliases.mapValuesTo(mutableMapOf()) { (_, names) -> names.toMutableSet() }
            val taken = commands.mapTo(mutableSetOf()) { neptuneName(it.name) }
            extra.values.forEach { taken += it }
            for (command in other.commands) {
                if (command.opcode !in opcodes) continue
                val name = neptuneName(command.name)
                if (!taken.add(name)) continue
                extra.getOrPut(command.opcode) { mutableSetOf() } += name
            }
            return Resolved(revision, commands, commitSha, extra)
        }

        fun writeCommandsSym(target: File) {
            target.parentFile.mkdirs()
            val lines = commands.sortedBy { it.opcode }.flatMap { command ->
                listOf("${command.opcode}\t${neptuneName(command.name)}") +
                    aliases[command.opcode].orEmpty().sorted().map { "${command.opcode}\t$it" }
            }
            target.writeText(lines.joinToString("\n", postfix = "\n"))
        }

        /** Writes one `.cs2` per zwyz source file, skipping core opcodes. */
        fun writePrelude(preludeDir: File) {
            preludeDir.mkdirs()
            val handlers = dynamicHandlers(revision)
            val byTarget = commands.filter { it.sourceFile != CORE_FILE }.groupBy { preludeFileFor(it.sourceFile) }
            for ((stem, group) in byTarget) {
                val sources = group.map { it.sourceFile }.distinct().sorted().joinToString(", ") { "data/commands/$it.txt" }
                val lines = mutableListOf(
                    "// Generated from zwyz/osrs-cache@${commitSha.take(8)} $sources for revision $revision.",
                    "// Do not edit; regenerate with ZwyzCommandTable.",
                    "",
                )
                for (command in group.sortedBy { it.opcode }) {
                    // Neptune allows no array types in command signatures before arrays v2, and no
                    // script from those revisions calls an array command.
                    if (revision < LegacyArraySyntax.ARRAYS_V2_REVISION && command.usesArrays()) continue
                    for (name in listOf(neptuneName(command.name)) + aliases[command.opcode].orEmpty().sorted()) {
                        val declaration = declaration(command, handlers, name)
                        lines += declaration
                        if (hasSecondary(name)) lines += declaration.replaceFirst("[command,", "[command,.")
                    }
                }
                File(preludeDir, "$stem.cs2").writeText(lines.joinToString("\n", postfix = "\n"))
            }
        }

        private fun Command.usesArrays(): Boolean =
            params.orEmpty().any { mapType(it.first).endsWith("array") } || returns.any { mapType(it).endsWith("array") }

        private fun declaration(command: Command, handlers: Set<String>, name: String): String {
            val head = "[command,$name]"
            val params = command.params ?: return head
            val mapped = uniqueNames(mapParams(name, params))
            val returns = command.returns.map { mapType(it) }
            val signature = buildString {
                if (mapped.isNotEmpty() || returns.isNotEmpty()) append("(").append(mapped.joinToString(", ") { (t, n) -> "$t $n" }).append(")")
                if (returns.isNotEmpty()) append("(").append(returns.joinToString(", ")).append(")")
            }
            return if (name in handlers) "$head/*$signature*/" else head + signature
        }

        private fun mapParams(name: String, params: List<Pair<String, String>>): List<Pair<String, String>> {
            val out = mutableListOf<Pair<String, String>>()
            var i = 0
            while (i < params.size) {
                val (type, param) = params[i]
                when {
                    type == "clientscript" && i + 1 < params.size && params[i + 1].first == "argument_list" -> {
                        out += hookType(name) to param
                        i += 2
                        continue
                    }
                    // implementation details of Neptune's own handling, not written in a signature
                    type == "basevartype" || type == "argument_list" -> Unit
                    else -> out += mapType(type) to param
                }
                i++
            }
            return out
        }

        /** zwyz occasionally repeats a parameter name (`long $x1, int $x1`); Neptune rejects that. */
        private fun uniqueNames(params: List<Pair<String, String>>): List<Pair<String, String>> {
            val seen = mutableSetOf<String>()
            return params.map { (type, name) ->
                var unique = name
                var n = 2
                while (!seen.add(unique)) unique = "${name}_${n++}"
                type to unique
            }
        }

        private fun hookType(name: String) = when {
            name.endsWith("stattransmit") -> "stathook"
            name.endsWith("invtransmit") -> "invhook"
            name.endsWith("vartransmit") -> "varphook"
            else -> "hook"
        }
    }

    /** Applies zwyz's revision rules and returns the effective command table for [revision]. */
    fun resolve(revision: Int): Resolved {
        val byName = LinkedHashMap<String, Command>()
        val byOpcode = HashMap<Int, Command>()
        for ((file, lines) in files) {
            for (raw in lines) {
                val line = raw.trim()
                if (line.isEmpty() || line.startsWith("//") || line.contains("(gap)")) continue
                val command = parse(line, file, revision) ?: continue
                byName[command.name] = command
                byOpcode[command.opcode] = command
            }
        }
        // A name whose opcode was later claimed by another name (e.g. db_find_with_count -> db_find at 228)
        // is no longer reachable by the decompiler, so it is dropped.
        val effective = byName.values.filter { byOpcode[it.opcode] === it }
        return Resolved(revision, effective, commitSha)
    }

    private fun parse(line: String, file: String, revision: Int): Command? {
        val match = LINE.matchEntire(line)
        if (match == null) {
            val bare = BARE.find(line) ?: error("Unparseable command line in $file: $line")
            return Command(bare.groupValues[1].toInt(), bare.groupValues[2], null, emptyList(), file)
        }
        val (opcode, name, params, returns, version) = match.destructured
        if (version.isNotEmpty() && version.toInt() > revision) return null
        val paramList = params.split(',').map { it.trim() }.filter { it.isNotEmpty() }.map {
            val parts = it.split(Regex("\\s+"))
            parts[0] to parts[1]
        }
        val returnList = returns.split(',').map { it.trim() }.filter { it.isNotEmpty() }
        return Command(opcode.toInt(), name, paramList, returnList, file)
    }

    companion object {
        const val REPO = "zwyz/osrs-cache"
        private const val CORE_FILE = "0_core"

        private val LINE = Regex("""^(\d+) \[command,([a-zA-Z0-9_]+)](?:\(([^)]*)\))?(?:\(([^)]*)\))?(?: (\d+))?$""")
        private val BARE = Regex("""^(\d+) \[command,([a-zA-Z0-9_]+)]""")

        /**
         * zwyz command name -> the name Neptune hardcodes for it. Applied to both `commands.sym` and
         * the prelude so the opcode and the declaration keep agreeing.
         */
        private val NAME_MAP = mapOf(
            // Neptune registers a dynamic handler under this name and looks the symbol up by it.
            "if_script_trigger" to "if_runscript*",
        )

        fun neptuneName(name: String): String = NAME_MAP[name] ?: name

        /** Commands that get a `.name` secondary variant, matching the hand-written Neptune prelude. */
        private val SECONDARY_NON_CC = setOf("if_find", "if_find_entityoverlay", "if_query_next", "text_pronoun", "pronoun")

        private fun hasSecondary(name: String) = name.startsWith("cc_") || name in SECONDARY_NON_CC

        /**
         * zwyz source file stem -> prelude file name, following the neptune-ps/clientscript layout.
         * Several zwyz files can merge into one prelude file. Stems not listed keep their own name
         * without the opcode prefix.
         */
        private val PRELUDE_FILES = mapOf(
            "100_interface_core" to "interface",
            "1000_interface_components" to "interface",
            "13000_debug" to "debug",
            "1820_deeplinks" to "deeplink",
            "3100_misc" to "misc",
            "6500_misc" to "misc",
            "3400_config_enums" to "enum",
            "3500_keyboard" to "keyboard",
            "3600_social" to "social",
            "3700_steam" to "steam",
            "3800_clans" to "clans",
            "3900_stockmarket" to "stockmarket",
            "4000_maths" to "math",
            "4100_strings" to "strings",
            "4200_config_objects" to "config_object",
            "4250_objunlock" to "objunlock",
            "5000_chat" to "chat",
            "5300_window" to "window",
            "5350_screenshot" to "screenshot",
            "5500_camera" to "camera",
            "5600_login" to "login",
            "6200_viewport" to "viewport",
            "6210_zoom" to "uizoom",
            "6220_safearea" to "safearea",
            "6230_sidebar" to "sidebar",
            "6600_worldmap" to "worldmap",
            "6700_clientop" to "clientop",
            "6750_npc" to "npc",
            "6800_loc" to "loc",
            "6850_obj" to "obj",
            "6900_player" to "player",
            "6950_tile" to "tile",
            "7000_highlight" to "highlight",
            "7060_objtags" to "objtags",
            "7100_minimenu" to "minimenu",
            "7120_objstack" to "objstack",
            "7200_entity_overlay" to "entityoverlay",
            "7250_minimap" to "minimap",
            "7400_string_vector" to "stringvector",
            "7500_dbtable" to "dbtable",
            "7600_loot_tracker" to "loot_tracker",
            "7800_hiscore" to "hiscore",
            "7900_worldentity" to "worldentity",
            "8000_array" to "array",
            "8200_plugin_setting" to "plugin_setting",
            "8300_plugin" to "plugin",
            "8500_group" to "group",
        )

        fun preludeFileFor(sourceFile: String): String = PRELUDE_FILES[sourceFile] ?: sourceFile.substringAfter('_')

        /** zwyz type -> Neptune type. Anything not listed passes through unchanged. */
        private val TYPE_MAP = mapOf(
            "unknown" to "any",
            "unknownarray" to "array",
            "unknown_int" to "int",
            "unknown_long" to "long",
            "bool" to "boolean",
            "intbool" to "int",
            "iftype" to "int", "rgb" to "int", "key" to "int",
            "chattype" to "int", "chatfilter" to "int",
            "opkind" to "int", "opmode" to "int",
            "setsize" to "int", "setposh" to "int", "setposv" to "int",
            "settextalignh" to "int", "settextalignv" to "int",
            "overlaytype" to "int", "gameoption" to "int", "deviceoption" to "int",
            "windowmode" to "int", "blendmode" to "int",
            "menuentrytype" to "int", "platformtype" to "int", "objowner" to "int", "clienttype" to "int",
            "clan" to "int", "group" to "int", "group_uid" to "int", "group_var" to "int",
            "worldentity" to "int",
        )

        fun mapType(type: String): String = TYPE_MAP[type] ?: type

        /**
         * Names registered through `addDynamicCommandHandler` in Neptune's `ClientScriptCompiler`,
         * gated the same way as `getDefaultFeaturesForVersion`.
         */
        fun dynamicHandlers(revision: Int): Set<String> {
            val set = mutableSetOf(
                "enum", "oc_param", "nc_param", "lc_param", "struct_param", "inv_param",
                "if_param", "cc_param", "if_setparam", "cc_setparam",
                "cc_find_param", "if_query_refine", "db_getfield",
                "db_find", "db_find_refine",
            )
            if (revision >= 230) set += "cc_create"
            if (revision < 228) set += listOf("db_find_with_count", "db_find_refine_with_count")
            if (revision >= 231) set += listOf(
                "array_compare", "array_indexof", "array_lastindexof", "array_count", "array_min", "array_max",
                "array_fill", "array_copy", "array_create", "array_push", "array_insert", "array_delete",
                "array_pushall", "array_insertall", "enum_getinputs", "enum_getoutputs",
            )
            return set
        }

        /** Downloads `data/commands` from the repository at [ref], a commit sha or branch name (`master` for the newest). */
        fun fetch(ref: String = "master"): ZwyzCommandTable {
            // A branch archive's folder is named after the branch, so resolve the sha first for provenance.
            val resolvedSha = if (ref.length == 40) ref else {
                val body = OsrsDumpsRevisionIndex.get("https://api.github.com/repos/$REPO/commits/$ref")
                com.google.gson.JsonParser.parseString(body).asJsonObject["sha"].asString
            }
            val temp = File.createTempFile("openrune-zwyz-", ".zip")
            try {
                downloadArchive(resolvedSha, temp)
                ZipFile(temp).use { zip ->
                    val rootPrefix = zip.fileHeaders.first().fileName.substringBefore('/') + "/"
                    val files = zip.fileHeaders
                        .filter { !it.isDirectory && it.fileName.removePrefix(rootPrefix).let { p -> p.startsWith("data/commands/") && p.endsWith(".txt") } }
                        .sortedBy { it.fileName.substringAfterLast('/').substringBefore('_').toInt() }
                        .map { header ->
                            val stem = header.fileName.substringAfterLast('/').removeSuffix(".txt")
                            stem to zip.getInputStream(header).bufferedReader().use { it.readLines() }
                        }
                    check(files.isNotEmpty()) { "No data/commands files found in $REPO@$resolvedSha" }
                    return ZwyzCommandTable(resolvedSha, files)
                }
            } finally {
                temp.delete()
            }
        }

        private fun downloadArchive(sha: String, destination: File) {
            val url = java.net.URL("https://codeload.github.com/$REPO/zip/$sha")
            val connection = (url.openConnection() as java.net.HttpURLConnection).apply {
                connectTimeout = 15_000
                readTimeout = 120_000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "OpenRune-FileStore")
            }
            try {
                check(connection.responseCode == 200) { "Failed to download $url: ${connection.responseCode}" }
                connection.inputStream.use { input -> destination.outputStream().use { input.copyTo(it) } }
            } finally {
                connection.disconnect()
            }
        }
    }
}
