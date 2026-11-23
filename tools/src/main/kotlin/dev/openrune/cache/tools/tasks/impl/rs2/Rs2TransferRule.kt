package dev.openrune.cache.tools.tasks.impl.rs2

import dev.openrune.filesystem.Cache
import dev.openrune.filesystem.Compression

enum class Rs2Payload { FILES, CONTAINER }

sealed interface Rs2TransferRule {

    val sourceIndex: Int
    val targetIndex: Int
    val label: String
    val key: String

    val newIndexCompression: Compression
    val newIndexNamed: Boolean

    fun signature(): String

    fun expand(from: Cache, targetIndex: Int): List<Rs2Copy>

    companion object {
        const val NEXT_FREE = -1
        const val INDEX_FLOOR = 30
    }

    data class Index(
        override val sourceIndex: Int,
        override val targetIndex: Int = NEXT_FREE,
        val payload: Rs2Payload = Rs2Payload.FILES,
        val archives: (Int) -> Boolean = { true },
        val archive: (Int) -> Int = { it },
        val file: (Int) -> Int = { it },
        override val newIndexCompression: Compression = Compression.GZIP,
        override val newIndexNamed: Boolean = false,
        override val label: String = "RS3 index $sourceIndex",
        override val key: String = label,
    ) : Rs2TransferRule {

        override fun signature() = "index|$sourceIndex|$targetIndex|$payload|${probe(archive)}|${probe(file)}"

        override fun expand(from: Cache, targetIndex: Int): List<Rs2Copy> =
            from.archives(sourceIndex)
                .filter(archives)
                .flatMap { sourceArchive -> expandArchive(from, targetIndex, sourceArchive, archive(sourceArchive)) }

        private fun expandArchive(
            from: Cache,
            targetIndex: Int,
            sourceArchive: Int,
            targetArchive: Int,
        ): List<Rs2Copy> =
            when (payload) {
                Rs2Payload.CONTAINER -> listOf(
                    Rs2Copy.Container(sourceIndex, sourceArchive, targetIndex, targetArchive, 0),
                )

                Rs2Payload.FILES -> from.files(sourceIndex, sourceArchive).map { sourceFile ->
                    Rs2Copy.File(
                        sourceIndex, sourceArchive, sourceFile,
                        targetIndex, targetArchive, file(sourceFile),
                    )
                }
            }
    }

    data class Archive(
        override val sourceIndex: Int,
        val sourceArchive: Int,
        override val targetIndex: Int = NEXT_FREE,
        val targetArchive: Int,
        val files: (Int) -> Boolean = { true },
        val file: (Int) -> Int = { it },
        override val newIndexCompression: Compression = Compression.GZIP,
        override val newIndexNamed: Boolean = false,
        override val label: String = "RS3 $sourceIndex/$sourceArchive",
        override val key: String = label,
    ) : Rs2TransferRule {

        override fun signature() =
            "archive|$sourceIndex:$sourceArchive|$targetIndex:$targetArchive|${probe(file)}"

        override fun expand(from: Cache, targetIndex: Int): List<Rs2Copy> =
            from.files(sourceIndex, sourceArchive)
                .filter(files)
                .map { sourceFile ->
                    Rs2Copy.File(
                        sourceIndex, sourceArchive, sourceFile,
                        targetIndex, targetArchive, file(sourceFile),
                    )
                }
    }

    data class Configs(
        override val sourceIndex: Int,
        val bits: Int,
        override val targetIndex: Int = NEXT_FREE,
        val targetArchive: Int,
        val id: (Int) -> Int = { it },
        val ids: (Int) -> Boolean = { true },
        override val newIndexCompression: Compression = Compression.GZIP,
        override val newIndexNamed: Boolean = false,
        override val label: String = "RS3 configs $sourceIndex",
        override val key: String = label,
    ) : Rs2TransferRule {

        override fun signature() =
            "configs|$sourceIndex:$bits|$targetIndex:$targetArchive|${probe(id)}"

        override fun expand(from: Cache, targetIndex: Int): List<Rs2Copy> =
            from.archives(sourceIndex).flatMap { archive ->
                from.files(sourceIndex, archive).toList().mapNotNull { file ->
                    val sourceId = (archive shl bits) + file
                    if (!ids(sourceId)) {
                        null
                    } else {
                        Rs2Copy.File(sourceIndex, archive, file, targetIndex, targetArchive, id(sourceId))
                    }
                }
            }
    }

    data class Group(
        override val sourceIndex: Int,
        val sourceArchive: Int,
        val sourceFile: Int = 0,
        override val targetIndex: Int = NEXT_FREE,
        val targetArchive: Int,
        val targetFile: Int = 0,
        val payload: Rs2Payload = Rs2Payload.FILES,
        override val newIndexCompression: Compression = Compression.GZIP,
        override val newIndexNamed: Boolean = false,
        override val label: String = "RS3 $sourceIndex/$sourceArchive/$sourceFile",
        override val key: String = label,
    ) : Rs2TransferRule {

        override fun signature() =
            "group|$sourceIndex:$sourceArchive:$sourceFile|$targetIndex:$targetArchive:$targetFile|$payload"

        override fun expand(from: Cache, targetIndex: Int): List<Rs2Copy> = when (payload) {
            Rs2Payload.CONTAINER -> listOf(
                Rs2Copy.Container(sourceIndex, sourceArchive, targetIndex, targetArchive, targetFile),
            )

            Rs2Payload.FILES -> listOf(
                Rs2Copy.File(
                    sourceIndex, sourceArchive, sourceFile,
                    targetIndex, targetArchive, targetFile,
                ),
            )
        }
    }
}

private fun probe(remap: (Int) -> Int): String =
    PROBE_IDS.joinToString(",") { runCatching { remap(it).toString() }.getOrDefault("?") }

private val PROBE_IDS = listOf(0, 1, 2, 7, 15, 64, 255, 1_000, 65_535, 1_048_576)

class Rs2TransferSpec internal constructor() {

    private val rules = mutableListOf<Rs2TransferRule>()

    fun rule(rule: Rs2TransferRule) {
        rules += rule
    }

    fun index(
        from: Int,
        to: Int = Rs2TransferRule.NEXT_FREE,
        payload: Rs2Payload = Rs2Payload.FILES,
        archives: (Int) -> Boolean = { true },
        archive: (Int) -> Int = { it },
        file: (Int) -> Int = { it },
        compression: Compression = Compression.GZIP,
        named: Boolean = false,
        label: String = "RS3 index $from",
        key: String = label,
    ) = rule(
        Rs2TransferRule.Index(
            sourceIndex = from,
            targetIndex = to,
            payload = payload,
            archives = archives,
            archive = archive,
            file = file,
            newIndexCompression = compression,
            newIndexNamed = named,
            label = label,
            key = key,
        ),
    )

    fun archive(
        fromIndex: Int,
        fromArchive: Int,
        toIndex: Int = Rs2TransferRule.NEXT_FREE,
        toArchive: Int = fromArchive,
        files: (Int) -> Boolean = { true },
        file: (Int) -> Int = { it },
        compression: Compression = Compression.GZIP,
        named: Boolean = false,
        label: String = "RS3 $fromIndex/$fromArchive",
        key: String = label,
    ) = rule(
        Rs2TransferRule.Archive(
            sourceIndex = fromIndex,
            sourceArchive = fromArchive,
            targetIndex = toIndex,
            targetArchive = toArchive,
            files = files,
            file = file,
            newIndexCompression = compression,
            newIndexNamed = named,
            label = label,
            key = key,
        ),
    )

    fun configs(
        fromIndex: Int,
        bits: Int,
        toIndex: Int = Rs2TransferRule.NEXT_FREE,
        toArchive: Int,
        ids: (Int) -> Boolean = { true },
        compression: Compression = Compression.GZIP,
        named: Boolean = false,
        label: String = "RS3 configs $fromIndex",
        key: String = label,
        id: (Int) -> Int = { it },
    ) = rule(
        Rs2TransferRule.Configs(
            sourceIndex = fromIndex,
            bits = bits,
            targetIndex = toIndex,
            targetArchive = toArchive,
            id = id,
            ids = ids,
            newIndexCompression = compression,
            newIndexNamed = named,
            label = label,
            key = key,
        ),
    )

    fun group(
        fromIndex: Int,
        fromArchive: Int,
        fromFile: Int = 0,
        toIndex: Int = Rs2TransferRule.NEXT_FREE,
        toArchive: Int = fromArchive,
        toFile: Int = fromFile,
        payload: Rs2Payload = Rs2Payload.FILES,
        compression: Compression = Compression.GZIP,
        named: Boolean = false,
        key: String = "RS3 $fromIndex/$fromArchive/$fromFile",
    ) = rule(
        Rs2TransferRule.Group(
            sourceIndex = fromIndex,
            sourceArchive = fromArchive,
            sourceFile = fromFile,
            targetIndex = toIndex,
            targetArchive = toArchive,
            targetFile = toFile,
            payload = payload,
            newIndexCompression = compression,
            newIndexNamed = named,
            key = key,
        ),
    )

    internal fun build(): List<Rs2TransferRule> = rules.toList()
}

fun rs2Transfer(source: Rs2Source, spec: Rs2TransferSpec.() -> Unit): Rs2Transfer =
    Rs2Transfer(source, Rs2TransferSpec().apply(spec).build())
