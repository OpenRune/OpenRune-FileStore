package dev.openrune.definition.constants

import java.io.File

/**
 * Interface for different mapping file formats
 */
interface MappingProvider {
    val mappings : MutableMap<String, MutableMap<String, Int>>
    fun load(vararg mappings: File)
    fun getSupportedExtensions(): List<String>
}