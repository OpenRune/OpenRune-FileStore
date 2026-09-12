package dev.openrune.definition.constants.impl

import dev.openrune.definition.constants.GameValWrite
import dev.openrune.definition.constants.MutableMappingProvider
import dev.openrune.definition.constants.UnassignedGameVal
import java.io.File

/**
 * Provider for RSCM format mapping files with auto-detection of v1 and v2 formats.
 * 
 * RSCM v1 format: key:value (colon-separated)
 * RSCM v2 format: key=value (equals-separated) with optional sub-properties: key:subprop=value
 * 
 * Supports both full type and base type mappings:
 * - Full type: filename.key -> value
 * - Base type: baseType.key -> value (removes _v1, _v2, etc. suffixes)
 * 
 * Also tracks sub-types for files that contain sub-property definitions.
 */
class RSCMProvider : MutableMappingProvider {
    override val mappings: MutableMap<String, MutableMap<String, Int>> = emptyMap<String, MutableMap<String, Int>>().toMutableMap()

    private val tableSources: MutableMap<String, File> = mutableMapOf()

    private val unassigned: MutableList<UnassignedGameVal> = mutableListOf()

    override fun load(vararg mappings: File) {
        require(mappings.isNotEmpty()) { "You need at least one mapping file" }
        val mappingsDir = mappings.first()

        require(mappingsDir.exists() && mappingsDir.isDirectory) {
            "Mappings directory does not exist or is not a directory: ${mappingsDir.absolutePath}"
        }

        tableSources.clear()
        unassigned.clear()

        mappingsDir.listFiles { _, name -> name.endsWith(".rscm") }?.forEach { file ->
            try {
                processRSCMFile(file)
            } catch (e: Exception) {
                throw IllegalArgumentException("Failed to process RSCM file: ${file.name}", e)
            }
        }
    }

    private fun processRSCMFile(file: File) {
        val fullType = file.nameWithoutExtension
        val baseType = extractBaseType(fullType)
        val lines = file.readLines().filter { it.isNotBlank() }

        if (lines.isEmpty()) return

        // Detect format based on first non-empty line
        val firstLine = lines.first()
        val format = detectFormat(firstLine, file.name)

        // Track sub-types for this file
        val fileSubTypes = mutableSetOf<String>()

        mappings[fullType] = emptyMap<String, Int>().toMutableMap()
        tableSources[fullType] = file

        lines.forEachIndexed { lineNumber, line ->
            try {
                val (key, value) = when (format) {
                    RSCMFormat.V1 -> parseRSCMV1Line(line, lineNumber + 1)
                    RSCMFormat.V2 -> parseRSCMV2Line(line, lineNumber + 1, fileSubTypes)
                }

                mappings[fullType]?.put("${fullType}.${key}",value)
                if (value == UNASSIGNED) {
                    unassigned += UnassignedGameVal(fullType, key, file)
                }
            } catch (e: Exception) {
                throw IllegalArgumentException(
                    "Failed to parse line ${lineNumber + 1} in ${file.name}: $line", e
                )
            }
        }
    }

    override fun unassignedGameVals(): List<UnassignedGameVal> = unassigned.toList()

    override fun sourceOf(table: String, key: String): File? {
        if (mappings[table]?.containsKey("$table.$key") != true) return null
        return tableSources[table]
    }

    override fun writeGameVals(entries: List<GameValWrite>) {
        val placed = entries.map { entry ->
            if (entry.source != null) entry else entry.copy(source = tableSources[entry.table])
        }

        placed.groupBy { it.source }.forEach { (file, fileEntries) ->
            if (file == null) {
                fileEntries.forEach {
                    println("No .rscm file for table '${it.table}', dropping gameval '${it.key}'")
                }
            } else {
                writeFile(file, fileEntries)
            }
        }

        placed.forEach { entry ->
            mappings.getOrPut(entry.table) { mutableMapOf() }["${entry.table}.${entry.key}"] = entry.id
            entry.source?.let { tableSources.putIfAbsent(entry.table, it) }
        }

        unassigned.removeAll { placeholder ->
            placed.any { it.table == placeholder.table && it.key == placeholder.key }
        }
    }

    private fun writeFile(file: File, entries: List<GameValWrite>) {
        val original = file.readText()
        val separator = if (original.contains("\r\n")) "\r\n" else "\n"
        val lines = original.lines().let { if (it.lastOrNull().isNullOrEmpty()) it.dropLast(1) else it }
            .toMutableList()
        val format = lines.firstOrNull { it.isNotBlank() }
            ?.let { detectFormat(it, file.name) }
            ?: RSCMFormat.V2

        fun indexOfKey(key: String): Int = lines.indexOfFirst { line ->
            line.isNotBlank() && runCatching { parseKeyOnly(line, format) }.getOrNull() == key
        }

        val (present, absent) = entries.partition { indexOfKey(it.key) != -1 }

        present.forEach { entry ->
            lines[indexOfKey(entry.key)] = formatLine(entry.key, entry.id, format)
        }

        absent.forEach { entry ->
            require(entry.after != null || entry.generated) {
                "Cannot assign '${entry.key}' in ${file.name}: that key is not declared in the file"
            }
            val anchor = entry.after?.let(::indexOfKey) ?: -1
            val line = formatLine(entry.key, entry.id, format)
            if (anchor == -1) lines.add(line) else lines.add(anchor + 1, line)
        }

        file.writeText(lines.joinToString(separator, postfix = separator))
    }

    private fun formatLine(key: String, id: Int, format: RSCMFormat): String = when (format) {
        RSCMFormat.V1 -> "$key:$id"
        RSCMFormat.V2 -> "$key=$id"
    }

    private fun parseKeyOnly(line: String, format: RSCMFormat): String = when (format) {
        RSCMFormat.V1 -> parseRSCMV1Line(line, 0).first
        RSCMFormat.V2 -> parseRSCMV2Line(line, 0, mutableSetOf()).first
    }

    private fun extractBaseType(filename: String): String {
        return filename.replace(Regex("_v\\d+$"), "")
    }

    private fun detectFormat(firstLine: String, fileName: String): RSCMFormat {
        return when {
            firstLine.contains("=") -> RSCMFormat.V2
            firstLine.contains(":") -> RSCMFormat.V1
            else -> throw IllegalArgumentException(
                "Unable to detect RSCM format from first line in $fileName: $firstLine"
            )
        }
    }

    private fun parseRSCMV1Line(line: String, lineNumber: Int): Pair<String, Int> {
        val parts = line.split(":")
        require(parts.size == 2) {
            "Invalid RSCM v1 line format at line $lineNumber. Expected 'key:value', got: $line"
        }
        return parts[0].trim() to parts[1].trim().toInt()
    }

    private fun parseRSCMV2Line(line: String, lineNumber: Int, subTypes: MutableSet<String>): Pair<String, Int> {
        return when {
            line.contains("=") -> {
                val parts = line.split("=")
                require(parts.size == 2) {
                    "Invalid RSCM v2 line format at line $lineNumber. Expected 'key=value', got: $line"
                }
                parts[0].trim() to parts[1].trim().toInt()
            }
            line.contains(":") -> {
                val parts = line.split(":")
                require(parts.size == 2) {
                    "Invalid RSCM v2 sub-property format at line $lineNumber. Expected 'key:subprop=value', got: $line"
                }
                val key = parts[0].trim()
                val valuePart = parts[1].trim()
                
                val valueParts = valuePart.split("=")
                require(valueParts.size == 2) {
                    "Invalid RSCM v2 sub-property value format at line $lineNumber. Expected 'subprop=value', got: $valuePart"
                }
                
                val subType = key
                subTypes.add(subType)
                
                key to valueParts[1].trim().toInt()
            }
            else -> throw IllegalArgumentException(
                "Invalid RSCM v2 line format at line $lineNumber. Expected 'key=value' or 'key:subprop=value', got: $line"
            )
        }
    }

    override fun getSupportedExtensions(): List<String> = listOf(".rscm", ".rscm2")

    private enum class RSCMFormat {
        V1, V2
    }

    private companion object {
        const val UNASSIGNED = -1
    }
}