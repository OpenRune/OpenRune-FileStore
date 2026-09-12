package dev.openrune.cache.tools.tasks.impl.defs

import com.github.michaelbull.logging.InlineLogger
import dev.openrune.cache.tools.incremental.Hashing
import dev.openrune.definition.constants.ConstantProvider
import java.io.File

internal data class RawBlock(
    val file: File,
    val name: String,
    val ordinal: Int,
    val id: Int?,
    val inherit: Int?,
    val serverOnly: Boolean,
    val fingerprint: String,

    val constants: Set<String>,
) {
    val matchKey: String get() = if (id != null) "$name#$id" else "$name@$ordinal"

    val unitKey: String
        get() = if (id != null) "$name#$id" else "${file.absolutePath.replace('\\', '/')}#$name@$ordinal"

    val label: String get() = "${file.name} (${id ?: name})"
}

internal object ConfigBlocks {
    private val logger = InlineLogger()

    private val BLOCK_HEADER = Regex("""^\[\[\s*([^\[\]]+?)\s*]]""")

    private val SUB_TABLE = Regex("""^\[[^\[]""")

    private val FIELD = Regex("""^"?(id|inherit|isServerOnly)"?\s*=\s*(.+?)\s*(?:#.*)?$""")

    private val QUOTED = Regex("\"([^\"]+)\"")

    private val TABLE_PREFIX = Regex("""[A-Za-z_][A-Za-z0-9_]*""")

    private const val TOKEN_BLOCK = "tokenizedreplacement"

    fun scan(file: File): List<RawBlock> {
        val text = runCatching { file.readText() }.getOrElse { failure ->
            logger.warn { "Could not read ${file.name}: ${failure.message}" }
            return emptyList()
        }
        return runCatching { scanText(file, text) }.getOrElse { failure ->
            logger.warn { "Could not scan ${file.name} into blocks: ${failure.message}" }
            emptyList()
        }
    }

    private fun scanText(file: File, text: String): List<RawBlock> {
        val sections = split(text)

        val tokenSections = sections.filter { it.name.lowercase() == TOKEN_BLOCK }
        val tokenFingerprint = tokenSections.joinToString("\n") { it.body }

        val tokenConstants = tokenSections.flatMapTo(HashSet()) { constantTokensIn(it.body) }

        val ordinals = HashMap<String, Int>()
        return sections.mapNotNull { section ->
            if (section.name.lowercase() == TOKEN_BLOCK) return@mapNotNull null
            val ordinal = ordinals.merge(section.name, 1, Int::plus)!! - 1
            RawBlock(
                file = file,
                name = section.name,
                ordinal = ordinal,
                id = section.fields["id"]?.let { intValueOf(it, file, section.name) },
                inherit = section.fields["inherit"]?.let { intValueOf(it, file, section.name) },
                serverOnly = section.fields["isServerOnly"]?.trim().equals("true", ignoreCase = true),
                fingerprint = fingerprintOf(section.body, tokenFingerprint, constantTokensIn(section.body) + tokenConstants),
                constants = constantTokensIn(section.body) + tokenConstants,
            )
        }
    }

    /**
     * Content hash of a block, mixing in the resolved value of every constant the block names. Renumbering
     * a gameval therefore changes the fingerprint of exactly the blocks that use it, so the block is stale
     * by the same check that catches an edit rather than only by the recorded dependency comparison.
     */
    private fun fingerprintOf(body: String, tokenFingerprint: String, constants: Set<String>): String {
        val resolved = constants.sorted().joinToString(",") { "$it=${ConstantProvider.peekMapping(it)}" }
        return Hashing.hashBytes("$body|$tokenFingerprint|$resolved".toByteArray())
    }

    private fun constantTokensIn(body: String): Set<String> =
        QUOTED.findAll(body)
            .map { it.groupValues[1] }
            .filter { token -> '.' in token && TABLE_PREFIX.matches(token.substringBefore('.')) }
            .toSet()

    private class Section(val name: String, val body: String, val fields: Map<String, String>)

    private fun split(text: String): List<Section> {
        val sections = mutableListOf<Section>()
        var name: String? = null
        var body = StringBuilder()
        var fields = HashMap<String, String>()
        var inSubTable = false

        fun flush() {
            name?.let { sections += Section(it, body.toString(), fields) }
            name = null
            body = StringBuilder()
            fields = HashMap()
            inSubTable = false
        }

        text.lineSequence().forEach { rawLine ->
            val line = rawLine.trim()
            if (line.isEmpty() || line.startsWith("#")) return@forEach

            val header = BLOCK_HEADER.find(line)
            if (header != null) {
                flush()
                name = header.groupValues[1]
                body.append(line).append('\n')
                return@forEach
            }

            if (name == null) return@forEach
            body.append(line).append('\n')

            if (SUB_TABLE.containsMatchIn(line)) {
                inSubTable = true
                return@forEach
            }
            if (inSubTable) return@forEach

            FIELD.find(line)?.let { match ->
                fields.putIfAbsent(match.groupValues[1], match.groupValues[2])
            }
        }

        flush()
        return sections
    }

    /**
     * Resolves an `id`/`inherit` value to a number. A quoted value is a constant token; failing to resolve
     * it is reported, because an unresolved id costs the block its stable identity — it falls back to a
     * position-based key that cannot notice the gameval being renumbered.
     */
    private fun intValueOf(raw: String, file: File, block: String): Int? {
        val value = raw.trim()
        value.toIntOrNull()?.let { return it }
        if (value.length >= 2 && value.startsWith('"') && value.endsWith('"')) {
            val token = value.substring(1, value.lastIndex)
            val resolved = ConstantProvider.peekMapping(token)
            if (resolved == null) {
                logger.warn {
                    "Incremental packing cannot resolve \"$token\" in ${file.name} [[$block]]; " +
                        "changes to that gameval will not be detected for this block"
                }
            }
            return resolved
        }
        return null
    }

    private data class Target(val index: Int, val archive: Int, val id: Int)

    fun inheritOrder(blocks: List<RawBlock>): (RawBlock) -> Int {
        val original = blocks.withIndex().associate { (index, block) -> block to index }

        if (blocks.none { it.inherit != null }) {
            return { block -> original[block] ?: 0 }
        }

        val producer = HashMap<Target, RawBlock>()
        blocks.forEach { block ->
            val id = block.id ?: return@forEach
            val target = targetOf(block.name, id) ?: return@forEach
            producer.putIfAbsent(target, block)
        }

        val parents = HashMap<RawBlock, RawBlock>()
        blocks.forEach { block ->
            val inherit = block.inherit ?: return@forEach
            val target = targetOf(block.name, inherit) ?: return@forEach
            val parent = producer[target] ?: return@forEach
            if (parent != block) parents[block] = parent
        }

        val depth = HashMap<RawBlock, Int>()
        val visiting = HashSet<RawBlock>()
        var reportedCycle = false

        fun resolve(block: RawBlock): Int {
            depth[block]?.let { return it }
            if (!visiting.add(block)) {
                if (!reportedCycle) {
                    logger.warn { "Inherit cycle involving ${block.label}; falling back to file order for it" }
                    reportedCycle = true
                }
                return 0
            }
            val value = parents[block]?.let { resolve(it) + 1 } ?: 0
            visiting.remove(block)
            depth[block] = value
            return value
        }

        blocks.forEach { resolve(it) }

        val span = blocks.size + 1

        return { block -> (depth[block] ?: 0) * span + (original[block] ?: 0) }
    }

    private fun targetOf(type: String, id: Int): Target? =
        PackConfig.packTypes[type]?.let { Target(it.index, it.archive, id) }
}
