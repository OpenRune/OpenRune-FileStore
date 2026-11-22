package dev.openrune.cache.tools.tasks.impl

import com.displee.cache.ProgressListener
import com.displee.compress.CompressionType
import dev.openrune.cache.CacheDelegate
import dev.openrune.cache.tools.tasks.CacheTask
import dev.openrune.filesystem.Cache

/*
 * Removes Bzip2 Compression and replaces it with Gzip.
 * This improves read performance.
 */
class RemoveBzip : CacheTask() {
    override fun init(cache: Cache) {
        val library = (cache as CacheDelegate).library

        // First pass: flag everything that needs recompressing, so the bar knows how much work there is.
        val targetType = CompressionType.GZIP
        val compressor = library.compressors.get(targetType)
        val touched = mutableListOf<com.displee.cache.index.Index>()
        var archives = 0
        for (index in library.indices()) {
            if (index.version == 0) { //empty index
                continue
            }
            var flagged = false
            if (index.compressionType == CompressionType.BZIP2) {
                index.compressionType = targetType
                index.compressor = compressor
                index.flag()
                flagged = true
            }
            for (archiveId in index.archiveIds()) {
                val archive = index.archive(archiveId) ?: continue
                if (archive.compressionType == CompressionType.BZIP2) {
                    archive.compressionType = targetType
                    archive.compressor = compressor
                    archive.flag()
                    archives++
                    flagged = true
                }
            }
            if (flagged) touched += index
        }
        if (touched.isEmpty()) return

        // Second pass: recompress, one step per archive so a long run visibly moves.
        val bar = progress.begin("Removing Bzip", archives.toLong())
        val listener = object : ProgressListener {
            override fun notify(progress: Double, message: String?) {
                if (message?.startsWith("Repacking archive") == true) bar.step()
            }
        }
        for (index in touched) {
            bar.message("index ${index.id}")
            index.update(listener)
        }
        bar.close()
    }
}
