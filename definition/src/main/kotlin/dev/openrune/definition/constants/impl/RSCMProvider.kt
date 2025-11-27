package dev.openrune.definition.constants.impl

import dev.openrune.definition.constants.MappingProvider
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
class RSCMProvider : MappingProvider {
    override val mappings: MutableMap<String, MutableMap<String, Int>> = emptyMap<String, MutableMap<String, Int>>().toMutableMap()

    override fun load(vararg mappings: File) {
        require(mappings.isNotEmpty()) { "You need at least one mapping file" }
        val mappingsDir = mappings.first()
        
        require(mappingsDir.exists() && mappingsDir.isDirectory) {
            "Mappings directory does not exist or is not a directory: ${mappingsDir.absolutePath}"
        }

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

        lines.forEachIndexed { lineNumber, line ->
            try {
                val (key, value) = when (format) {
                    RSCMFormat.V1 -> parseRSCMV1Line(line, lineNumber + 1)
                    RSCMFormat.V2 -> parseRSCMV2Line(line, lineNumber + 1, fileSubTypes)
                }

                mappings[fullType]?.put("${fullType}.${key}",value)
            } catch (e: Exception) {
                throw IllegalArgumentException(
                    "Failed to parse line ${lineNumber + 1} in ${file.name}: $line", e
                )
            }
        }
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
}