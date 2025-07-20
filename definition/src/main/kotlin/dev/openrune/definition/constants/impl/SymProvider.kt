package dev.openrune.definition.constants.impl

import dev.openrune.definition.constants.MappingProvider
import java.io.File

/**
 * Provider for Sym format mapping files.
 * 
 * Sym format: key\tvalue (tab-separated)
 * Example: abyssal_whip	4151
 * 
 * Supports both full type and base type mappings:
 * - Full type: filename.key -> value
 * - Base type: baseType.key -> value (removes _v1, _v2, etc. suffixes)
 */
class SymProvider : MappingProvider {
    
    override fun load(mappingsDir: File): Map<String, Int> {
        require(mappingsDir.exists() && mappingsDir.isDirectory) {
            "Mappings directory does not exist or is not a directory: ${mappingsDir.absolutePath}"
        }
        
        val allMappings = mutableMapOf<String, Int>()
        
        mappingsDir.listFiles { _, name -> name.endsWith(".sym") }?.forEach { file ->
            try {
                processSymFile(file, allMappings)
            } catch (e: Exception) {
                throw IllegalArgumentException("Failed to process Sym file: ${file.name}", e)
            }
        }
        
        return allMappings
    }
    
    private fun processSymFile(file: File, mappings: MutableMap<String, Int>) {
        val fullType = file.nameWithoutExtension
        val baseType = extractBaseType(fullType)
        
        file.readLines()
            .filter { it.isNotBlank() }
            .forEachIndexed { lineNumber, line ->
                try {
                    val (key, value) = parseSymLine(line, lineNumber + 1)
                    val cleanKey = key.trim()
                    val cleanValue = value.trim().toInt()
                    
                    // Store mappings with both full type and base type prefixes
                    mappings["${fullType}.${cleanKey}"] = cleanValue
                    mappings["${baseType}.${cleanKey}"] = cleanValue
                } catch (e: Exception) {
                    throw IllegalArgumentException(
                        "Failed to parse line ${lineNumber + 1} in ${file.name}: $line", e
                    )
                }
            }
    }
    
    private fun parseSymLine(line: String, lineNumber: Int): Pair<String, String> {
        val parts = line.split("\t")
        require(parts.size == 2) {
            "Invalid Sym line format at line $lineNumber. Expected 'key\\tvalue', got: $line"
        }
        return parts[0] to parts[1]
    }
    
    private fun extractBaseType(filename: String): String {
        return filename.replace(Regex("_v\\d+$"), "")
    }
    
    override fun getSupportedExtensions(): List<String> = listOf(".sym")
}