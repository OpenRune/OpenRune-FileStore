package dev.openrune.definition.constants

import dev.openrune.definition.constants.impl.RSCMProvider
import dev.openrune.definition.constants.impl.SymProvider
import java.io.File

object ConstantProvider {
    var mappings: Map<String, Int> = emptyMap()
    var rscmTypes: MutableList<String> = mutableListOf()

    private var mappingProvider: MappingProvider = CompositeMappingProvider(
        listOf(RSCMProvider(), SymProvider())
    )

    fun load(mappingsDir: File) = load(mappingsDir, mappingProvider)
    
    fun load(mappingsDir: File, provider: MappingProvider) {
        require(mappingsDir.exists()) { "Missing Mapping Files" }
        
        mappings = provider.load(mappingsDir)
        rscmTypes.clear()
        rscmTypes.addAll(mappings.keys.map { it.substringBefore(".") }.distinct())
    }
    
    fun setMappingProvider(provider: MappingProvider) {
        mappingProvider = provider
    }
    
    fun getMappingProvider(): MappingProvider = mappingProvider

    fun overrideProviders(providers: List<MappingProvider>) {
        mappingProvider = CompositeMappingProvider(providers)
    }
    
    fun addProvider(provider: MappingProvider) {
        val current = mappingProvider
        mappingProvider = when (current) {
            is CompositeMappingProvider -> {
                current.addProvider(provider)
                current
            }
            else -> CompositeMappingProvider(listOf(current, provider))
        }
    }
    
    fun removeProvider(providerClass: Class<out MappingProvider>) {
        val current = mappingProvider
        mappingProvider = when (current) {
            is CompositeMappingProvider -> {
                current.removeProvider(providerClass)
                if (current.providers.isEmpty()) CompositeMappingProvider(emptyList()) else current
            }
            else -> if (current::class.java == providerClass) {
                CompositeMappingProvider(emptyList())
            } else current
        }
    }
    
    fun removeProvider(provider: MappingProvider) {
        val current = mappingProvider
        mappingProvider = when (current) {
            is CompositeMappingProvider -> {
                current.removeProvider(provider)
                if (current.providers.isEmpty()) CompositeMappingProvider(emptyList()) else current
            }
            else -> if (current == provider) {
                CompositeMappingProvider(emptyList())
            } else current
        }
    }
    
    fun clearProviders() {
        mappingProvider = CompositeMappingProvider(emptyList())
    }

    fun resetToDefaults() {
        mappingProvider = CompositeMappingProvider(listOf(RSCMProvider(), SymProvider()))
    }

    fun getCurrentProviders(): List<MappingProvider> {
        val current = mappingProvider
        return when (current) {
            is CompositeMappingProvider -> current.providers
            else -> listOf(current)
        }
    }


    fun getMapping(key: String): Int {
        val value = mappings[key] ?: error("Missing mapping for key: '$key'")
        return value
    }
    
    fun getMappingOrNull(key: String): Int? = mappings[key]
    
    /**
     * Extension function to get sub-types from a complex type mapping.
     * Usage: ConstantProvider.getMapping("base.type").subtype(0) returns the type name (e.g., "int", "string")
     */
    fun Int.subtype(index: Int): String {
        // Find the key that maps to this value
        val key = mappings.entries.find { it.value == this }?.key
        if (key != null) {
            // Try to get type info from SymProvider
            val symProvider = getCurrentProviders().find { it is SymProvider } as? SymProvider
            val typeInfo = symProvider?.getTypeInfo(key)
            
            if (typeInfo != null) {
                if (typeInfo.contains(",")) {
                    // Complex type with multiple sub-types
                    val subTypes = typeInfo.split(",").map { it.trim() }
                    if (index < subTypes.size) {
                        return subTypes[index]
                    }
                } else {
                    // Simple type with single type - only return it for index 0
                    if (index == 0) {
                        return typeInfo.trim()
                    }
                }
            }
        }
        // Fallback - return "unknown" if no type info found or invalid index
        return "unknown"
    }
}
