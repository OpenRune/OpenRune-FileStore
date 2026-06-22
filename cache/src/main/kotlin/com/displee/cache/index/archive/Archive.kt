package com.displee.cache.index.archive

import com.displee.cache.index.archive.file.File
import org.openrs2.cache.Js5CompressionType
import java.util.*

open class Archive(val id: Int, var hashName: Int = 0, xtea: IntArray? = null) : Comparable<Archive> {

    var compressionType: Js5CompressionType = Js5CompressionType.GZIP

    var revision = 0
    private var needUpdate = false
    val files: SortedMap<Int, File> = TreeMap()

    var crc: Int = 0
    var whirlpool: ByteArray? = null
    var checksum = 0
    var length = 0
    var uncompressedLength = 0

    private var _xtea: IntArray? = xtea
    var xtea: IntArray?
        get() = _xtea
        set(value) {
            // only flag the archive for update when it was read and the xtea got changed
            if (read && !(_xtea contentEquals value)) {
                //bzip2 compression fails when xteas are set for some reason, cheap fix
                compressionType = Js5CompressionType.GZIP
                flag()
            }
            _xtea = value
        }

    var read = false
    var new = false
    var autoUpdateRevision = true

    constructor(id: Int) : this(id, 0)

    constructor(archive: Archive) : this(archive.id, archive.hashName) {
        for (file in archive.files()) {
            files[file.id] = File(file)
        }
        revision = archive.revision
        crc = archive.crc
        whirlpool = archive.whirlpool?.clone()
        xtea = archive.xtea?.clone()
    }

    override fun compareTo(other: Archive): Int {
        return id.compareTo(other.id)
    }

    fun containsData(): Boolean {
        for (entry in files.values) {
            if (entry.data != null) {
                return true
            }
        }
        return false
    }

    @JvmOverloads
    fun add(vararg files: File, overwrite: Boolean = true): Array<File> {
        val newFiles = ArrayList<File>(files.size)
        files.forEach { newFiles.add(add(it, overwrite)) }
        return newFiles.toTypedArray()
    }

    @JvmOverloads
    fun add(file: File, overwrite: Boolean = true): File {
        return add(file.id, checkNotNull(file.data) { "File data is null." }, file.hashName, overwrite)
    }

    fun add(data: ByteArray): File {
        return add(nextId(), data)
    }

    @JvmOverloads
    fun add(name: String, data: ByteArray, overwrite: Boolean = true): File {
        var id = fileId(name)
        if (id == -1) {
            id = nextId()
        }
        return add(id, data, toHash(name), overwrite)
    }

    @JvmOverloads
    fun add(id: Int, data: ByteArray, hashName: Int = -1, overwrite: Boolean = true): File {
        var file = files[id]
        if (file == null) {
            file = File(id, data, if (hashName == -1) 0 else hashName)
            files[id] = file
            flag()
        } else if (overwrite) {
            var flag = false
            if (!Arrays.equals(file.data, data)) {
                file.data = data
                flag = true
            }
            if (hashName != -1 && file.hashName != hashName) {
                file.hashName = hashName
                flag = true
            }
            if (flag) {
                flag()
            }
        }
        return file
    }

    fun file(id: Int): File? {
        return files[id]
    }

    fun file(data: ByteArray): File? {
        return files.filterValues { Arrays.equals(it.data, data) }.values.firstOrNull()
    }

    fun file(name: String): File? {
        return files.filterValues { it.hashName == toHash(name) }.values.firstOrNull()
    }

    fun contains(id: Int): Boolean {
        return files.containsKey(id)
    }

    fun contains(name: String): Boolean {
        return fileId(name) != -1
    }

    fun remove(id: Int): File? {
        val file = files.remove(id) ?: return null
        flag()
        return file
    }

    fun remove(name: String): File? {
        return remove(fileId(name))
    }

    fun first(): File? {
        if (files.isEmpty()) {
            return null
        }
        return file(files.firstKey())
    }

    fun last(): File? {
        if (files.isEmpty()) {
            return null
        }
        return files[files.lastKey()]
    }

    fun fileId(name: String): Int {
        val hashName = toHash(name)
        files.values.forEach {
            if (it.hashName == hashName) {
                return it.id
            }
        }
        return -1
    }

    fun nextId(): Int {
        val last = last()
        return if (last == null) 0 else last.id + 1
    }

    fun copyFiles(): Array<File> {
        val files = files()
        val copy = ArrayList<File>(files.size)
        for (i in files.indices) {
            copy.add(i, File(files[i]))
        }
        return copy.toTypedArray()
    }

    fun flag() {
        needUpdate = true
    }

    fun flagged(): Boolean {
        return needUpdate
    }

    fun unFlag() {
        if (!flagged()) {
            return
        }
        needUpdate = false
    }

    fun restore() {
        for (file in files.values) {
            file.data = null
        }
        read = false
        new = false
    }

    /**
     * Clear the files.
     */
    fun clear() {
        files.clear()
    }

    fun fileIds(): IntArray {
        return files.keys.toIntArray()
    }

    fun files(): Array<File> {
        return files.values.toTypedArray()
    }

    @Deprecated("Use property syntax", ReplaceWith("xtea"))
    fun xtea(xtea: IntArray?) {
        this.xtea = xtea
    }

    @Deprecated("Use property syntax", ReplaceWith("xtea"))
    fun xtea(): IntArray? {
        return xtea
    }

    open fun toHash(name: String): Int {
        return name.hashCode()
    }

    override fun toString(): String {
        return "Archive[id=$id, hash_name=$hashName, revision=$revision, crc=$crc, has_whirlpool=${whirlpool != null}, read=$read, files_size=${files.size}]"
    }
}