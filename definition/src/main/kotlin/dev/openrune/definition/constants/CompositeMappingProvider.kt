package dev.openrune.definition.constants

import java.io.File


/**
 * Composite provider that can handle multiple formats
 */
class CompositeMappingProvider(providers: List<MappingProvider>) : MappingProvider {
    private val _providers = providers.toMutableList()
    
    val providers: List<MappingProvider> get() = _providers.toList()
    
    override fun load(mappingsDir: File): Map<String, Int> {
        val allMappings = mutableMapOf<String, Int>()
        
        _providers.forEach { provider ->
            allMappings.putAll(provider.load(mappingsDir))
        }
        
        return allMappings
    }
    
    override fun getSupportedExtensions(): List<String> {
        return _providers.flatMap { it.getSupportedExtensions() }
    }
    
    override fun getSubTypes(filePrefix: String): Set<String> {
        return _providers.flatMap { it.getSubTypes(filePrefix) }.toSet()
    }
    
    /**
     * Add a provider to this composite
     */
    fun addProvider(provider: MappingProvider) {
        _providers.add(provider)
    }
    
    /**
     * Remove a provider by class type
     */
    fun removeProvider(providerClass: Class<out MappingProvider>) {
        _providers.removeAll { it::class.java == providerClass }
    }
    
    /**
     * Remove a specific provider instance
     */
    fun removeProvider(provider: MappingProvider) {
        _providers.remove(provider)
    }
    
    /**
     * Clear all providers
     */
    fun clearProviders() {
        _providers.clear()
    }
    
    /**
     * Check if this composite contains a specific provider type
     */
    fun containsProvider(providerClass: Class<out MappingProvider>): Boolean {
        return _providers.any { it::class.java == providerClass }
    }
    
    /**
     * Get providers of a specific type
     */
    fun <T : MappingProvider> getProvidersOfType(providerClass: Class<T>): List<T> {
        return _providers.filterIsInstance(providerClass)
    }
} 