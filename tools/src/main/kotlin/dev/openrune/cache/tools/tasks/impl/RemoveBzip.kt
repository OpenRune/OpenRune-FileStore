package dev.openrune.cache.tools.tasks.impl

import com.displee.cache.CacheLibrary
import com.displee.compress.CompressionType
import dev.openrune.cache.tools.tasks.CacheTask
import dev.openrune.cache.util.progress

/*
 * Removes Bzip2 Compression and replaces it with Gzip.
 * This improves read performance.
 */
class RemoveBzip : CacheTask() {
    override fun init(library: CacheLibrary) {
        val progress = progress("Removing Bzip compression, step:", 2)
        //for the first step loop through and flag the files.
        var indices = 0
        var archives = 0
        val targetType = CompressionType.GZIP
        val compressor = library.compressors.get(targetType)
        for (index in library.indices()) {
            if (index.version == 0) { //empty index
                continue
            }
            if (index.compressionType == CompressionType.BZIP2) {
                index.compressionType = targetType
                index.compressor = compressor
                index.flag()
                indices++
            }
            for (archiveId in index.archiveIds()) {
                val archive = index.archive(archiveId) ?: continue
                if (archive.compressionType == CompressionType.BZIP2) {
                    archive.compressionType = targetType
                    archive.compressor = compressor
                    archive.flag()
                    archives++
                }
            }
        }
        progress.extraMessage = " from $archives archives and $indices indices."
        progress.step()
        //for the next step actually update the cache
        if (indices > 0 || archives > 0) {
            library.update()
        }
        progress.step()
        progress.close()
    }
}