package dev.openrune.cache.tools.tasks.impl.rs2

import com.github.michaelbull.logging.InlineLogger
import dev.openrune.cache.tools.incremental.PackUnit
import dev.openrune.cache.tools.tasks.CacheTask
import dev.openrune.filesystem.Cache
import java.io.File
import java.security.MessageDigest

class Rs2Transfer(
    private val source: Rs2Source,
    private val rules: List<Rs2TransferRule>,
) : CacheTask() {

    private val logger = InlineLogger()

    val indices: Map<String, Int> get() = resolved

    private val resolved = LinkedHashMap<String, Int>()

    override fun init(cache: Cache) {
        if (rules.isEmpty()) return

        resolved.clear()
        val from = source.open()
        try {
            val sourceFingerprint = fingerprint(from)
            val taken = mutableSetOf<Int>()
            rules.forEachIndexed { position, rule ->
                val targetIndex = resolveIndex(cache, rule, taken)
                resolved[rule.key] = targetIndex
                runRule(cache, from, rule, position, targetIndex, sourceFingerprint)
            }
        } finally {
            from.close()
        }
    }

    private fun resolveIndex(target: Cache, rule: Rs2TransferRule, taken: MutableSet<Int>): Int {
        if (rule.targetIndex != Rs2TransferRule.NEXT_FREE) {
            taken += rule.targetIndex
            return rule.targetIndex
        }
        var id = Rs2TransferRule.INDEX_FLOOR
        while (target.exists(id) || id in taken) id++
        taken += id
        return id
    }

    private fun runRule(
        target: Cache,
        from: Cache,
        rule: Rs2TransferRule,
        position: Int,
        targetIndex: Int,
        sourceFingerprint: String,
    ) {
        if (!from.exists(rule.sourceIndex)) {
            logger.warn { "${rule.label}: source index ${rule.sourceIndex} does not exist, skipped" }
            return
        }

        val copies = rule.expand(from, targetIndex)
        if (copies.isEmpty()) {
            logger.warn { "${rule.label}: nothing matched in source index ${rule.sourceIndex}" }
            return
        }

        ensureIndex(target, rule, targetIndex)

        val signature = rule.signature()
        val units = copies.map { copy ->
            PackUnit(
                key = "$position|${copy.key()}",
                sources = emptyList(),
                label = copy.label(),
                fingerprint = "$sourceFingerprint|$signature|${copy.key()}",
            )
        }
        val byKey = copies.associateBy { "$position|${it.key()}" }

        incremental.run(
            task = this,
            scope = "${source.id}#$position",
            label = rule.label,
            cache = target,
            units = units,
        ) { packCache, unit ->
            copy(from, packCache, byKey.getValue(unit.key))
        }
    }

    private fun ensureIndex(target: Cache, rule: Rs2TransferRule, targetIndex: Int) {
        if (target.exists(targetIndex)) return
        target.createIndex(
            compressionType = rule.newIndexCompression,
            named = rule.newIndexNamed,
            id = targetIndex,
        )
        logger.info { "${rule.label}: created index $targetIndex" }
    }

    private fun copy(from: Cache, target: Cache, copy: Rs2Copy) {
        when (copy) {
            is Rs2Copy.File -> {
                val data = from.data(copy.sourceIndex, copy.sourceArchive, copy.sourceFile)
                    ?: error("No data at ${copy.sourceIndex}/${copy.sourceArchive}/${copy.sourceFile}")
                target.write(copy.targetIndex, copy.targetArchive, copy.targetFile, data)
            }

            is Rs2Copy.Container -> {
                val data = from.sector(copy.sourceIndex, copy.sourceArchive)
                    ?: error("No container at ${copy.sourceIndex}/${copy.sourceArchive}")
                target.write(copy.targetIndex, copy.targetArchive, copy.targetFile, data)
            }
        }
    }

    private fun fingerprint(from: Cache): String {
        val table = runCatching { from.versionTable }.getOrNull()
        if (table == null || table.isEmpty()) {
            logger.warn { "Source ${source.id} has no version table; transfers will repack every build" }
            return "unversioned:${System.nanoTime()}"
        }
        return MessageDigest.getInstance("SHA-256").digest(table).joinToString("") { "%02x".format(it) }
    }
}

class Rs2Source(val id: String, private val open: () -> Cache) {

    fun open(): Cache = open.invoke()

    companion object {
        fun local(directory: File): Rs2Source =
            Rs2Source(directory.absolutePath) { Cache.load(directory.toPath()) }
    }
}

sealed interface Rs2Copy {
    val sourceIndex: Int
    val sourceArchive: Int
    val targetIndex: Int
    val targetArchive: Int
    val targetFile: Int

    fun key(): String
    fun label(): String

    data class File(
        override val sourceIndex: Int,
        override val sourceArchive: Int,
        val sourceFile: Int,
        override val targetIndex: Int,
        override val targetArchive: Int,
        override val targetFile: Int,
    ) : Rs2Copy {
        override fun key() = "$sourceIndex:$sourceArchive:$sourceFile"
        override fun label() = "$sourceIndex/$sourceArchive/$sourceFile -> $targetIndex/$targetArchive/$targetFile"
    }

    data class Container(
        override val sourceIndex: Int,
        override val sourceArchive: Int,
        override val targetIndex: Int,
        override val targetArchive: Int,
        override val targetFile: Int,
    ) : Rs2Copy {
        override fun key() = "$sourceIndex:$sourceArchive:container"
        override fun label() = "$sourceIndex/$sourceArchive (container) -> $targetIndex/$targetArchive/$targetFile"
    }
}
