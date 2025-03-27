package dev.openrune.filesystem

import dev.openrune.filesystem.util.secure.VersionTableBuilder
import java.io.File
import java.io.FileNotFoundException
import java.io.RandomAccessFile
import java.util.*

interface CacheLoader {

    fun load(
        properties: Properties,
        xteas: Map<Int, IntArray>? = null,
        mapFactory: MapFactory = ::createNameMap
    ): Cache {
        val cachePath = properties.getProperty("cachePath")
        val threadUsage = properties.getProperty("threadUsage", "1.0").toDouble()
        return load(cachePath, threadUsage = threadUsage, mapFactory = mapFactory)
    }

    fun load(
        path: String,
        xteas: Map<Int, IntArray>? = null,
        threadUsage: Double = 1.0,
        mapFactory: MapFactory = ::createNameMap
    ): Cache {
        val mainFile = File(path, "${FileCache.CACHE_FILE_NAME}.dat2")
        if (!mainFile.exists()) {
            throw FileNotFoundException("Main file not found at '${mainFile.absolutePath}'.")
        }
        val main = RandomAccessFile(mainFile, "r")
        val index255File = File(path, "${FileCache.CACHE_FILE_NAME}.idx255")
        if (!index255File.exists()) {
            throw FileNotFoundException("Checksum file not found at '${index255File.absolutePath}'.")
        }
        val index255 = RandomAccessFile(index255File, "r")
        val indexCount = index255.length().toInt() / ReadOnlyCache.INDEX_SIZE
        val versionTable = VersionTableBuilder(indexCount)
        return load(path, mainFile, main, index255File, index255, indexCount, versionTable, xteas, threadUsage, mapFactory)
    }

    fun load(
        path: String,
        mainFile: File,
        main: RandomAccessFile,
        index255File: File,
        index255: RandomAccessFile,
        indexCount: Int,
        versionTable: VersionTableBuilder? = null,
        xteas: Map<Int, IntArray>? = null,
        threadUsage: Double = 1.0,
        mapFactory: MapFactory = ::createNameMap
    ): Cache

    private fun createNameMap(): MutableMap<Int, Int> {
        return try {
            val fastutilClass = Class.forName("it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap")
            val constructor = fastutilClass.getConstructor(Int::class.java)
            constructor.newInstance(16384) as MutableMap<Int, Int>
        } catch (e: ClassNotFoundException) {
            LinkedHashMap(16384)
        }
    }
}