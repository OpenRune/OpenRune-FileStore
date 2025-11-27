package dev.openrune.definition.constants

import java.io.File

class CompositeMappingProvider(providers: List<MappingProvider>) : MappingProvider {
    override val mappings: MutableMap<String, MutableMap<String, Int>> = mutableMapOf()

    private val _providers = providers.toMutableList()

    val providers: List<MappingProvider> get() = _providers.toList()

    override fun load(vararg mappings: File) {
        this.mappings.clear()

        _providers.forEach { provider ->
            provider.load(*mappings)
            provider.mappings.forEach { (table, entries) ->
                val tableMappings = this.mappings.getOrPut(table) { mutableMapOf() }
                tableMappings.putAll(entries)
            }
        }
    }

    override fun getSupportedExtensions(): List<String> =
        _providers.flatMap { it.getSupportedExtensions() }

    fun addProvider(provider: MappingProvider) {
        if (_providers.any { it::class.java == provider::class.java }) return
        _providers.add(provider)
    }

    fun replaceProviders(providers: List<MappingProvider>) {
        _providers.clear()
        _providers.addAll(providers)
    }

    fun removeProvider(providerClass: Class<out MappingProvider>) {
        _providers.removeAll { it::class.java == providerClass }
    }

    fun removeProvider(provider: MappingProvider) {
        _providers.remove(provider)
    }

    fun clearProviders() {
        _providers.clear()
    }

    fun containsProvider(providerClass: Class<out MappingProvider>): Boolean =
        _providers.any { it::class.java == providerClass }

    fun <T : MappingProvider> getProvidersOfType(providerClass: Class<T>): List<T> =
        _providers.filterIsInstance(providerClass)
}