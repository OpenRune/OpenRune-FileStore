package dev.openrune.cache.tools.tasks.impl.rs2

import com.displee.cache.CacheLibrary
import dev.openrune.Rs2Cache
import dev.openrune.cache.CacheDelegate
import dev.openrune.cache.tools.CachePaths
import dev.openrune.filesystem.Cache
import dev.openrune.rs2.Rs2Content
import java.io.File

object Rs2Staging {

    fun stage(
        configs: List<Rs2Content> = emptyList(),
        archives: List<Int> = emptyList(),
        rs2CacheId: Int,
        limit: Int = NO_LIMIT,
        groups: Map<Int, Set<Int>> = emptyMap(),
    ): File {
        val filters = groups.entries.sortedBy { it.key }
            .joinToString("-") { "idx${it.key}:${it.value.size}:${it.value.sorted().hashCode()}" }
        val key = (configs.map { it.name } + archives.map { "idx$it" }).sorted().joinToString("-") + "@$limit@$filters"
        val staging = File(CachePaths.baseDir(), "caches/rs2/$rs2CacheId/${key.hashCode().toUInt().toString(16)}")
        val marker = File(staging, "staged.txt")
        if (marker.isFile) return staging

        staging.deleteRecursively()
        val rs2 = Rs2Cache.loadRemote(id = rs2CacheId)
        try {
            val indices = configs.mapTo(LinkedHashSet()) { it.index } + archives
            val cache = freshCache(staging, indices)
            try {
                configs.forEach { type ->
                    val ids = rs2.configIds(type.location)
                    println("Staging ${ids.size} ${type.name}")
                    ids.forEach { id ->
                        val bytes = rs2.configBytes(type.location, id)
                        cache.write(type.index, type.archiveOf(id), type.fileOf(id), bytes)
                    }
                }
                archives.forEach { index ->
                    if (!rs2.hasIndex(index)) {
                        println("Staging index $index: not in this cache, skipped")
                        return@forEach
                    }
                    val wanted = groups[index]
                    val available = rs2.groups(index)
                    val selected = (if (wanted == null) available else available.filter { it in wanted })
                        .let { if (limit == NO_LIMIT) it else it.take(limit) }
                    if (wanted != null) {
                        println("Staging ${selected.size} of ${available.size} groups of index $index")
                    } else {
                        println("Staging ${selected.size} groups of index $index")
                    }
                    val failed = mutableListOf<Int>()
                    selected.forEach { group ->
                        try {
                            rs2.files(index, group).forEach { file ->
                                cache.write(index, group, file, rs2.groupBytes(index, group, file))
                            }
                        } catch (e: Exception) {
                            failed += group
                        }
                    }
                    if (failed.isNotEmpty()) {
                        println("Staging index $index: ${failed.size} unreadable groups skipped ${failed.take(20)}")
                    }
                }
                cache.update()
            } finally {
                cache.close()
            }
        } finally {
            rs2.close()
        }
        marker.writeText(key)
        return staging
    }

    const val NO_LIMIT = -1

    private fun freshCache(directory: File, indices: Collection<Int>): Cache {
        directory.mkdirs()
        File(directory, CacheLibrary.MAIN_FILE_FILE_NAME).createNewFile()
        File(directory, CacheLibrary.INDEX_255_FILE_NAME).createNewFile()
        CacheLibrary(directory.absolutePath).use { library ->
            indices.forEach { if (!library.exists(it)) library.createIndex(id = it) }
            library.update()
        }
        return CacheDelegate(CacheLibrary(directory.absolutePath))
    }
}
