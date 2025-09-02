package dev.openrune.definition.constants.impl

import dev.openrune.definition.constants.MappingProvider
import java.io.File

/**
 * Provider for Sym format mapping files.
 * 
 * Sym format: ID\tname[\ttype] (tab-separated, type is optional)
 * Example: 4151	abyssal_whip	int
 * Example: 4151	abyssal_whip (type optional for sub-types)
 * 
 * For complex types with multiple sub-types:
 * Example: 253968	clan_setting_options_list:clan_setting_option	int,string,graphic
 * 
 * For individual sub-types (type is optional):
 * Example: 253969	clan_setting_options_list:clan_setting_option:0	int
 * Example: 253969	clan_setting_options_list:clan_setting_option:0 (type optional)
 * 
 * Supports both full type and base type mappings:
 * - Full type: filename.key -> value
 * - Base type: baseType.key -> value (removes _v1, _v2, etc. suffixes)
 */
class SymProvider : MappingProvider {
    
    // Store type information for complex types
    private val typeInfo = mutableMapOf<String, String>()
    
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
                    val (key, value, typeInfo) = parseSymLine(line, lineNumber + 1)
                    val cleanKey = key.trim()
                    val cleanValue = value.trim().toInt()
                    
                    // Store mappings with both full type and base type prefixes
                    mappings["${fullType}.${cleanKey}"] = cleanValue
                    mappings["${baseType}.${cleanKey}"] = cleanValue
                    
                    if (typeInfo != null) {
                        this.typeInfo["${fullType}.${cleanKey}"] = typeInfo
                        this.typeInfo["${baseType}.${cleanKey}"] = typeInfo
                    }

                } catch (e: Exception) {
                    throw IllegalArgumentException(
                        "Failed to parse line ${lineNumber + 1} in ${file.name}: $line", e
                    )
                }
            }
    }
    
    private fun parseSymLine(line: String, lineNumber: Int): Triple<String, String, String?> {
        val parts = line.split("\t")
        require(parts.size >= 2) {
            "Invalid Sym line format at line $lineNumber. Expected 'ID\\tname[\\ttype]', got: $line"
        }
        val typeInfo = if (parts.size >= 3) parts[2].trim() else null
        return Triple(parts[1], parts[0], typeInfo)
    }
    
    private fun extractBaseType(filename: String): String {
        return filename.replace(Regex("_v\\d+$"), "")
    }
    
    override fun getSupportedExtensions(): List<String> = listOf(".sym")
    
    /**
     * Get type information for a mapping key
     */
    fun getTypeInfo(key: String): String? = typeInfo[key]
}