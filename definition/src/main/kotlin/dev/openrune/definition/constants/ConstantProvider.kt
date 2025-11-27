package dev.openrune.definition.constants

import com.github.michaelbull.logging.InlineLogger
import dev.openrune.definition.constants.impl.RSCMProvider
import dev.openrune.definition.constants.impl.SymProvider
import java.io.File
import kotlin.system.measureNanoTime
import kotlin.system.measureTimeMillis

object ConstantProvider {
    internal val logger = InlineLogger()

    lateinit var provider: MappingProvider
        private set

    var mappings: Map<String, MutableMap<String, Int>> = emptyMap()

    val types: MutableList<String> = mutableListOf()

    private var mappingProvider: MappingProvider = defaultProviders()

    fun load(mappingsDir: File) = load(mappingsDir, mappingProvider)

    fun load(provider: MappingProvider) {
        val timeNs = measureNanoTime {
            provider.assertHasMappings()
            this.provider = provider
            mappings = provider.mappings
            refreshTypes(provider)
        }
        val timeLog = if (timeNs < 1_000_000) "$timeNs ns" else "${timeNs / 1_000_000} ms"
        logger.info { "Loaded provider '${provider::class.simpleName}' with ${mappings.size} tables in $timeLog" }
    }

    fun load(mappingsDir: File, provider: MappingProvider) {
        val timeNs = measureNanoTime {
            provider.load(mappingsDir)
            load(provider)
        }
        val timeLog = if (timeNs < 1_000_000) "$timeNs ns" else "${timeNs / 1_000_000} ms"
        logger.info { "Loaded provider '${provider::class.simpleName}' from directory '${mappingsDir.path}' in $timeLog" }
    }

    inline fun <reified T : MappingProvider> loadWith(
        providerFactory: () -> T,
        vararg files: File
    ) = providerFactory().also { it.load(*files) }.let(::load)

    fun loadWith(provider: MappingProvider, vararg files: File) =
        provider.also { it.load(*files) }.let(::load)

    fun setMappingProvider(provider: MappingProvider) {
        mappingProvider = provider
    }

    fun getMappingProvider(): MappingProvider = mappingProvider

    fun overrideProviders(providers: List<MappingProvider>) {
        mappingProvider = when (providers.size) {
            0 -> CompositeMappingProvider(emptyList())
            1 -> providers.first()
            else -> CompositeMappingProvider(providers)
        }
    }

    fun addProvider(provider: MappingProvider) {
        val composite = ensureComposite()
        composite.addProvider(provider)
    }

    fun removeProvider(providerClass: Class<out MappingProvider>) {
        val composite = ensureComposite()
        composite.removeProvider(providerClass)
    }

    fun removeProvider(provider: MappingProvider) {
        val composite = ensureComposite()
        composite.removeProvider(provider)
    }

    fun clearProviders() {
        overrideProviders(emptyList())
    }

    fun resetToDefaults() {
        mappingProvider = defaultProviders()
    }

    fun getCurrentProviders(): List<MappingProvider> = when (val current = mappingProvider) {
        is CompositeMappingProvider -> current.providers
        else -> listOf(current)
    }

    fun getReverseMapping(table: String, key: Int): String {
        ensureProviderInitialized()
        val tableMappings = provider.mappings[table] ?: error("Missing table '$table'")

        return tableMappings.entries
            .firstOrNull { it.value == key }
            ?.key ?: error("Missing mapping '$key' in table '$table'")
    }

    fun getMapping(key: String): Int {
        ensureProviderInitialized()
        val tableMappings = provider.tableFor(key)
        return tableMappings[key] ?: error("Missing mapping for key: '$key'")
    }

    fun getMappingOrNull(key: String): Int? {
        if (!::provider.isInitialized) return null
        val tableMappings = provider.mappings[key.tableName()] ?: return null
        return tableMappings[key]
    }

    private fun ensureComposite(): CompositeMappingProvider {
        val current = mappingProvider
        if (current is CompositeMappingProvider) return current
        return CompositeMappingProvider(listOf(current)).also { mappingProvider = it }
    }

    private fun refreshTypes(source: MappingProvider) {
        types.clear()
        types.addAll(source.mappings.keys)
    }

    private fun MappingProvider.assertHasMappings() {
        if (mappings.isEmpty()) error("Provider has not loaded any mappings")
    }

    private fun MappingProvider.tableFor(key: String): MutableMap<String, Int> {
        val tableName = key.tableName()
        return mappings[tableName] ?: error("Missing table '$tableName'")
    }

    private fun defaultProviders(): MappingProvider =
        CompositeMappingProvider(listOf(RSCMProvider(), SymProvider()))

    private fun String.tableName(): String = substringBefore(".")

    private fun ensureProviderInitialized() {
        check(::provider.isInitialized) { "ConstantProvider.load(...) must be called before lookups" }
    }
}

/**
 * Extension function on MappingProvider to load files and apply the provider to ConstantProvider.
 * This provides a concise way to create, load, and use a provider in one call.
 * 
 * @param files The files to load into the provider
 * 
 * @example
 * ```
 * // Instead of:
 * // val provider = GameValProvider()
 * // provider.load(file1, file2)
 * // ConstantProvider.load(provider)
 * 
 * // You can simply do:
 * GameValProvider().use(file1, file2)
 * ```
 */
fun MappingProvider.use(vararg files: File) {
    load(*files)
    ConstantProvider.load(this)
}
