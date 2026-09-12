package dev.openrune.cache.tools.autocert

import com.github.michaelbull.logging.InlineLogger
import dev.openrune.cache.CONFIGS
import dev.openrune.cache.ITEM
import dev.openrune.cache.util.getFiles
import dev.openrune.definition.codec.ItemCodec
import dev.openrune.definition.constants.ConstantProvider
import dev.openrune.definition.constants.MutableMappingProvider
import dev.openrune.definition.type.ItemType
import dev.openrune.definition.type.ObjStackability
import dev.openrune.filesystem.Cache
import dev.openrune.toml.model.TomlValue
import dev.openrune.toml.rsconfig.decodeRuneScapeBlocks
import dev.openrune.toml.rsconfig.rsconfig
import dev.openrune.toml.tomlMapper
import dev.openrune.toml.util.InternalAPI

class CertCandidateScanner(private val settings: AutoCertSettings) {

    private val logger = InlineLogger()

    private val mapper = tomlMapper { rsconfig { } }

    @OptIn(InternalAPI::class)
    fun scan(cache: Cache?, revision: Int): List<CertCandidate> {
        val codec = ItemCodec(revision)
        val prefix = "${settings.table}."
        val baseIdCeiling = baseIdCeiling()
        val candidates = LinkedHashMap<String, CertCandidate>()

        settings.configDirectories.flatMap { getFiles(it, "toml") }.forEach { file ->
            mapper.decodeRuneScapeBlocks(escapeConstantTokens(file.readText())).forEach { block ->
                if (block.name != "item") return@forEach
                val properties = block.map.properties

                val rawId = properties["id"]
                if (rawId !is TomlValue.String) {
                    if (rawId != null) {
                        logger.info { "Auto-cert skipped an item in ${file.name}: its id is not a gameval" }
                    }
                    return@forEach
                }
                if (!rawId.value.startsWith(prefix)) return@forEach

                val itemKey = rawId.value.removePrefix(prefix)
                if (itemKey.startsWith(settings.keyPrefix)) return@forEach
                if (isBaseGameItem(rawId.value, baseIdCeiling)) return@forEach
                if (properties.bool("autoCert") == false) return@forEach
                if (properties.containsKey("noteTemplateId")) return@forEach
                if (properties.containsKey("placeholderTemplate")) return@forEach
                if (properties.containsKey("dummyitem")) return@forEach
                if (stackable(properties, cache, codec)) return@forEach

                val certKey = settings.keyPrefix + itemKey
                candidates[itemKey] = CertCandidate(itemKey, certKey, file)
            }
        }

        return candidates.values.toList()
    }

    /**
     * The scanner needs the raw gameval (`id = "obj.foo"`), so [mapper] deliberately runs without the
     * constant provider; rsconfig rejects constant-like tokens in that mode. Escaping the dots as
     * unicode escapes hides the tokens from that check, and TOML unescapes them again while
     * parsing, so the decoded string values are unchanged.
     */
    private fun escapeConstantTokens(input: String): String {
        if ('"' !in input) return input

        return buildString(input.length) {
            var cursor = 0
            while (cursor < input.length) {
                val quoteStart = input.indexOf('"', cursor)
                if (quoteStart < 0) {
                    append(input, cursor, input.length)
                    break
                }

                append(input, cursor, quoteStart)
                val quoteEnd = findStringEnd(input, quoteStart + 1)
                if (quoteEnd < 0) {
                    append(input, quoteStart, input.length)
                    break
                }

                val token = input.substring(quoteStart + 1, quoteEnd)
                if (looksLikeConstant(token)) {
                    append('"')
                    append(token.replace(".", "\\u002E"))
                    append('"')
                } else {
                    append(input, quoteStart, quoteEnd + 1)
                }
                cursor = quoteEnd + 1
            }
        }
    }

    private fun looksLikeConstant(token: String): Boolean {
        if ('.' !in token) return false
        val tablePrefix = token.substringBefore('.').trim()
        return constantTablePrefix.matches(tablePrefix)
    }

    private fun findStringEnd(input: String, start: Int): Int {
        var escaped = false
        var index = start
        while (index < input.length) {
            val ch = input[index]
            if (escaped) {
                escaped = false
            } else if (ch == '\\') {
                escaped = true
            } else if (ch == '"') {
                return index
            }
            index++
        }
        return -1
    }

    private fun baseIdCeiling(): Int =
        ConstantProvider.loadedProviders()
            .filterIsInstance<MutableMappingProvider>()
            .maxOfOrNull { it.maxBaseId(settings.table) }
            ?: -1

    private fun isBaseGameItem(gameval: String, baseIdCeiling: Int): Boolean {
        if (baseIdCeiling < 0) return false
        val id = ConstantProvider.getMappingOrNull(gameval) ?: return false
        return id in 0..baseIdCeiling
    }

    private fun stackable(
        properties: Map<String, TomlValue>,
        cache: Cache?,
        codec: ItemCodec,
    ): Boolean {
        when (val stacks = properties["stacks"]) {
            is TomlValue.String -> return stacks.value.equals(ObjStackability.Always.name, ignoreCase = true)
            is TomlValue.Integer -> return stacks.value.toInt() == ObjStackability.Always.id
            else -> Unit
        }

        val inherited = inherited(properties, cache, codec) ?: return false
        return inherited.stacks == ObjStackability.Always || inherited.noteTemplateId > 0
    }

    private fun inherited(
        properties: Map<String, TomlValue>,
        cache: Cache?,
        codec: ItemCodec,
    ): ItemType? {
        if (cache == null) return null
        val inherit = properties["inherit"] ?: return null
        val id = when (inherit) {
            is TomlValue.String -> ConstantProvider.getMappingOrNull(inherit.value)
            is TomlValue.Integer -> inherit.value.toInt()
            else -> null
        } ?: return null
        if (id < 0) return null
        val data = cache.data(CONFIGS, ITEM, id) ?: return null
        return codec.loadData(id, data)
    }

    private fun Map<String, TomlValue>.bool(key: String): Boolean? =
        (this[key] as? TomlValue.Bool)?.value

    private companion object {
        /** Same shape rsconfig uses: constants are `table.name`, prose with a period must not match. */
        private val constantTablePrefix = Regex("^[a-zA-Z][a-zA-Z0-9_]*$")
    }
}
