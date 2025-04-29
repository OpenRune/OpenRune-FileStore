package dev.openrune.cache.util

import dev.openrune.cache.tools.typeDumper.Language
import java.util.*

class Namer {

    val used = mutableSetOf<String>()

    fun name(name: String?, id: String, language: Language = Language.JAVA, writeToJava: Boolean = true): String? {
        var fname = name
        if (fname == null) return null
        if (fname.isBlank()) {
            fname = "UNKNOWN"
        }
        val sanitizedName = when (language) {
            Language.RSCM -> if (writeToJava) sanitizeRSCM(fname) else fname
            else -> sanitize(fname)
        } ?: return null

        if (language != Language.RSCM) {
            var uniqueName = sanitizedName
            if (used.contains(uniqueName)) {
                uniqueName += "_$id"
            }
            used.add(uniqueName)
            return uniqueName
        }

        return sanitizedName
    }


    companion object {

        fun sanitizeRSCM(value: String): String {

            if (value.matches(Regex("[A-Z_][A-Z0-9_]*"))) {
                return value
            }

            var formatted = value
                .uppercase()
                .replace(Regex("[^A-Z0-9_]"), "_")
                .replace(Regex("_+"), "_")
                .trim('_')

            // If it starts with a digit after formatting, prepend an underscore
            if (formatted.firstOrNull()?.isDigit() == true) {
                formatted = "_$formatted"
            }

            return formatted
        }

        fun formatForClassName(value: String): String {
            var value1 = value
            if (value.isBlank()) {
                value1 = "UNKOWN"
            }
            val prefix = Regex("^_\\d*").find(value1)?.value.orEmpty()

            // Remove the prefix from the original to process the rest
            val remainder = value1.removePrefix(prefix)

            // Split by underscores or non-alphanumeric characters
            val words = remainder
                .lowercase()
                .split(Regex("[^a-zA-Z0-9]+"))
                .filter { it.isNotBlank() }


            val capitalized = words.joinToString("") { it.replaceFirstChar(Char::uppercaseChar) }

            var formatted = prefix + capitalized


            if (formatted.firstOrNull()?.isDigit() == true) {
                formatted = "_$formatted"
            }

            return formatted
        }

        private fun sanitize(input: String?): String? {
            if (input == null) return null

            val sanitized = removeTags(input)
                .uppercase(Locale.getDefault())
                .replace(' ', '_')
                .replace("[^A-Z0-9_]".toRegex(), "")

            if (sanitized.isEmpty()) return null

            return if (sanitized.first().isDigit()) "_$sanitized" else sanitized
        }

        fun removeTags(str: String): String {
            val builder = StringBuilder(str.length)
            var inTag = false

            for (char in str) {
                when (char) {
                    '<' -> inTag = true
                    '>' -> inTag = false
                    else -> if (!inTag) builder.append(char)
                }
            }

            return builder.toString()
        }
    }
}