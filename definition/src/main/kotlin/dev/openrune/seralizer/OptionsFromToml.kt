package dev.openrune.seralizer

import dev.openrune.toml.model.TomlValue
import dev.openrune.definition.EntityOpsDefinition

/**
 * Parses option/ioption-style TOML into a fixed five-slot [MutableList] (indices 0–4).
 * Mirrors legacy `option1`..`option5` keys and validates suffix range.
 */
object StringListOptionsFromToml {

    fun apply(
        list: MutableList<String?>,
        id: Int,
        name: String,
        keyName: String,
        content: Map<String, TomlValue>,
    ) {
        val context = buildString {
            append("[$id]")
            if (name.isNotBlank()) append(" [$name]")
        }

        val invalidKeys =
            content.keys.filter { it.startsWith(keyName) }.mapNotNull {
                val suffix = it.removePrefix(keyName).toIntOrNull()
                if (suffix != null && (suffix < 1 || suffix > 5)) suffix else null
            }

        if (invalidKeys.isNotEmpty()) {
            val sorted = invalidKeys.sorted()
            val keyString = sorted.joinToString()
            when {
                sorted.any { it < 1 } -> println("$context Warning: Invalid keys for '$keyName': $keyString — indices must start at 1.")
                sorted.any { it > 5 } -> println("$context Warning: Invalid keys for '$keyName': $keyString — indices must not exceed 5.")
                else -> println("Warning: Invalid option key(s): ${sorted.joinToString()}.")
            }
            return
        }

        for (i in 1..5) {
            val key = "$keyName$i"
            val value = content[key]
            if (value != null) {
                list[i - 1] = (value as? TomlValue.String)?.value
            }
        }
    }
}

/**
 * Parses entity option TOML (option/subop/multiop/multisubop formats) into [EntityOpsDefinition].
 */
object EntityOpsOptionsFromToml {

    fun apply(
        ops: EntityOpsDefinition,
        id: Int,
        name: String,
        keyName: String,
        content: Map<String, TomlValue>,
    ) {
        val context = buildString {
            append("[$id]")
            if (name.isNotBlank()) append(" [$name]")
        }

        val invalidKeys =
            content.keys.filter { it.startsWith(keyName) }.mapNotNull {
                val suffix = it.removePrefix(keyName).toIntOrNull()
                if (suffix != null && (suffix < 1 || suffix > 5)) suffix else null
            }
        if (invalidKeys.isNotEmpty()) {
            val sorted = invalidKeys.sorted()
            val keyString = sorted.joinToString()
            when {
                sorted.any { it < 1 } -> println("$context Warning: Invalid keys for '$keyName': $keyString — indices must start at 1.")
                sorted.any { it > 5 } -> println("$context Warning: Invalid keys for '$keyName': $keyString — indices must not exceed 5.")
                else -> println("Warning: Invalid option key(s): ${sorted.joinToString()}.")
            }
            return
        }
        for (i in 1..5) {
            val key = "$keyName$i"
            val value = content[key]
            val text = (value as? TomlValue.String)?.value ?: continue
            ops.setOp(i - 1, text)
        }

        ((content[keyName] as? TomlValue.List)?.elements ?: emptyList()).forEach { element ->
            val map = (element as? TomlValue.Map)?.properties ?: return@forEach
            val slot = ((map["slot"] as? TomlValue.Integer)?.value?.toInt() ?: return@forEach) - 1
            val text = (map["text"] as? TomlValue.String)?.value ?: return@forEach
            if (slot in 0..4) {
                ops.setOp(slot, text)
            }
        }

        val subOpKeyRegex = Regex("""^subop([1-5])$""")
        content.forEach { (rawKey, rawValue) ->
            val match = subOpKeyRegex.matchEntire(rawKey) ?: return@forEach
            val slot = match.groupValues[1].toInt() - 1
            val rows = (rawValue as? TomlValue.List)?.elements ?: return@forEach
            rows.forEach { row ->
                val map = (row as? TomlValue.Map)?.properties ?: return@forEach
                val subId = ((map["index"] as? TomlValue.Integer)?.value?.toInt() ?: return@forEach) - 1
                val text = (map["text"] as? TomlValue.String)?.value ?: return@forEach
                if (slot in 0..4 && subId >= 0) {
                    ops.setSubOp(slot, subId, text)
                }
            }
        }

        val conditionalOpKeyRegex = Regex("""^multiop([1-5])$""")
        content.forEach { (rawKey, rawValue) ->
            val match = conditionalOpKeyRegex.matchEntire(rawKey) ?: return@forEach
            val slot = match.groupValues[1].toInt() - 1
            val rows: List<TomlValue> =
                when (rawValue) {
                    is TomlValue.Map -> listOf(rawValue)
                    is TomlValue.List -> rawValue.elements
                    else -> emptyList()
                }
            rows.forEach { row ->
                val map = (row as? TomlValue.Map)?.properties ?: return@forEach
                val text = (map["text"] as? TomlValue.String)?.value ?: return@forEach
                val varp = (map["varp"] as? TomlValue.Integer)?.value?.toInt() ?: 0
                val varbit = (map["varbit"] as? TomlValue.Integer)?.value?.toInt() ?: 0
                val min = (map["min"] as? TomlValue.Integer)?.value?.toInt() ?: 0
                val max = (map["max"] as? TomlValue.Integer)?.value?.toInt() ?: 0
                if (slot in 0..4) {
                    ops.setConditionalOp(slot, text, varp, varbit, min, max)
                }
            }
        }

        val compactKeyRegex = Regex("""^multisubop([1-5])$""")
        content.forEach { (rawKey, rawValue) ->
            val match = compactKeyRegex.matchEntire(rawKey) ?: return@forEach
            val slot = match.groupValues[1].toInt() - 1
            val rows = (rawValue as? TomlValue.List)?.elements ?: return@forEach
            rows.forEach { row ->
                val map = (row as? TomlValue.Map)?.properties ?: return@forEach
                val subId = ((map["index"] as? TomlValue.Integer)?.value?.toInt() ?: return@forEach) - 1
                val text = (map["text"] as? TomlValue.String)?.value ?: return@forEach
                val varp = (map["varp"] as? TomlValue.Integer)?.value?.toInt() ?: 0
                val varbit = (map["varbit"] as? TomlValue.Integer)?.value?.toInt() ?: 0
                val min = (map["min"] as? TomlValue.Integer)?.value?.toInt() ?: 0
                val max = (map["max"] as? TomlValue.Integer)?.value?.toInt() ?: 0
                if (slot in 0..4 && subId >= 0) {
                    ops.setConditionalSubOp(slot, subId, text, varp, varbit, min, max)
                }
            }
        }
    }
}
