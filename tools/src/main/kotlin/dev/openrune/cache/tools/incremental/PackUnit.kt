package dev.openrune.cache.tools.incremental

import java.io.File
import java.security.MessageDigest

data class PackUnit(
    val key: String,
    val sources: List<File>,
    val label: String = key,
    val fingerprint: String? = null,
) {
    constructor(key: String, source: File) : this(key, listOf(source), key)
}

data class CacheTarget(val index: Int, val archive: Int, val file: Int)

internal data class GameValEmit(
    val group: String,
    val kind: String,
    val name: String,
    val id: Int,
    val subId: Int,
)

internal class UnitRecord {
    val outputs = LinkedHashMap<CacheTarget, Int>()
    val constants = LinkedHashMap<String, Int?>()
    val cacheReads = LinkedHashSet<CacheTarget>()
    val gameVals = LinkedHashSet<GameValEmit>()
}

/** A cache config tracked by the gameval reference index: [kind] is a `ConfigRefKind` name. */
internal data class ConfigKey(val kind: String, val id: Int)

/**
 * One gameval-typed value inside a config. [slot] says where in the config it sits, [table] and [name]
 * identify the gameval, [id] is the id the config held when last seen.
 */
internal data class ConfigRef(val slot: String, val table: String, val name: String, val id: Int)

internal class StoredConfigRefs(val crc: Int, val refs: List<ConfigRef>)

internal class StoredUnit(
    val rowId: Long,
    val hash: String,
    val outputs: Map<CacheTarget, Int>,
    val constants: Map<String, Int?>,
    val cacheReads: Set<CacheTarget>,
    val gameVals: List<GameValEmit>,
    val extraFiles: Map<String, String>,
)

internal object Hashing {
    /** [onFile] runs after each file is folded in, for progress reporting on large trees. */
    fun hashFiles(files: List<File>, onFile: () -> Unit = {}): String {
        val digest = MessageDigest.getInstance("SHA-256")
        files.sortedBy { it.invariantPath() }.forEach { file ->
            onFile()
            digest.update(file.invariantPath().toByteArray())
            if (file.isFile) {
                file.inputStream().use { input ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val read = input.read(buffer)
                        if (read <= 0) break
                        digest.update(buffer, 0, read)
                    }
                }
            } else {
                digest.update(MISSING)
            }
        }
        return digest.digest().toHex()
    }

    fun hashBytes(data: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(data).toHex()

    fun hashTree(root: File, extensions: Set<String>? = null): String {
        if (!root.exists()) return "absent"
        if (root.isFile) return hashFiles(listOf(root))
        val files = root.walkTopDown()
            .filter { it.isFile }
            .filter { extensions == null || it.extension.lowercase() in extensions }
            .toList()
        return hashFiles(files)
    }

    private val MISSING = "<missing>".toByteArray()

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    fun File.invariantPath(): String = absolutePath.replace('\\', '/')
}
