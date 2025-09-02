package dev.openrune.definition.constants

import java.io.File

/**
 * Interface for different mapping file formats
 */
interface MappingProvider {
    fun load(mappingsDir: File): Map<String, Int>
    fun getSupportedExtensions(): List<String>
    fun getSubTypes(filePrefix: String): Set<String> = emptySet()
}