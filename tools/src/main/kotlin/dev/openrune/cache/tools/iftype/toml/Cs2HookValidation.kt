package dev.openrune.cache.tools.iftype.toml

import java.io.File

internal val SCRIPT_NAME_TABLES = setOf("clientscript", "ifscript", "script")

internal val QUOTED_STRING = Regex("\"([^\"]*)\"")

private val BRACKETED_SYM_NAME = Regex("""^\[\w+,(.+)]$""")

private val CS2_HEADER = Regex("""^\[\w+,([A-Za-z0-9_]+)]\(([^)]*)\)""")

private val CS2_PARAM = Regex("""([A-Za-z0-9_]+)\s*\$\w+""")

private val PARAM_TYPE_TABLES: Map<String, Set<String>> = mapOf(
    "graphic" to setOf("sprites", "sprite"),
    "sprite" to setOf("sprites", "sprite"),
    "model" to setOf("models", "model"),
    "seq" to setOf("seq", "sequences", "animation"),
    "animation" to setOf("seq", "sequences", "animation"),
    "texture" to setOf("textures", "texture"),
)

fun indexCs2ParamTypes(cs2Directory: File): Map<String, List<String>> {
    if (!cs2Directory.exists()) return emptyMap()
    val result = mutableMapOf<String, List<String>>()
    cs2Directory.walkTopDown()
        .filter { it.isFile && it.extension.equals("cs2", ignoreCase = true) }
        .forEach { file ->
            file.forEachLine { rawLine ->
                val match = CS2_HEADER.find(rawLine.trim()) ?: return@forEachLine
                val name = match.groupValues[1]
                val params = CS2_PARAM.findAll(match.groupValues[2]).map { it.groupValues[1] }.toList()
                result[name] = params
            }
        }
    return result
}

fun validateCs2HookArgs(raw: String, cs2ParamTypes: Map<String, List<String>>): List<String> {
    if (cs2ParamTypes.isEmpty()) return emptyList()
    val problems = mutableListOf<String>()

    for (match in HOOK_ARRAY.findAll(raw)) {
        val elements = splitArrayElements(match.groupValues[1])
        if (elements.isEmpty()) continue

        val firstQuoted = QUOTED_STRING.matchEntire(elements[0].trim())?.groupValues?.get(1) ?: continue
        val table = firstQuoted.substringBefore('.', missingDelimiterValue = "")
        if (table !in SCRIPT_NAME_TABLES) continue

        val scriptName = firstQuoted.substringAfter('.', missingDelimiterValue = "").let { name ->
            BRACKETED_SYM_NAME.matchEntire(name)?.groupValues?.get(1) ?: name
        }
        val declaredParams = cs2ParamTypes[scriptName] ?: continue

        val args = elements.drop(1)
        if (args.size != declaredParams.size) {
            problems += "Hook array \"$firstQuoted\" passes ${args.size} argument(s) but the script declares " +
                "${declaredParams.size} parameter(s)."
            continue
        }

        args.forEachIndexed { index, element ->
            val ref = QUOTED_STRING.matchEntire(element.trim())?.groupValues?.get(1) ?: return@forEachIndexed
            val paramType = declaredParams[index]
            val allowedTables = PARAM_TYPE_TABLES[paramType] ?: return@forEachIndexed
            val refTable = ref.substringBefore('.', missingDelimiterValue = "")
            if (refTable !in allowedTables) {
                problems += "Hook array \"$firstQuoted\" argument ${index + 1} (\"$ref\") is from table " +
                    "\"$refTable\", but the script declares that parameter as \"$paramType\" (expected " +
                    "${allowedTables.joinToString("/") { "$it.*" }})."
            }
        }
    }

    return problems
}

private val HOOK_ARRAY = Regex("""\[([^\[\]]*)]""")

private fun splitArrayElements(body: String): List<String> {
    val elements = mutableListOf<String>()
    val current = StringBuilder()
    var inQuotes = false
    for (c in body) {
        when {
            c == '"' -> {
                inQuotes = !inQuotes
                current.append(c)
            }
            c == ',' && !inQuotes -> {
                elements += current.toString()
                current.clear()
            }
            else -> current.append(c)
        }
    }
    if (current.toString().isNotBlank()) elements += current.toString()
    return elements
}
