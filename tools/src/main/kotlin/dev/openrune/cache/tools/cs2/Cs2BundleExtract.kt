package dev.openrune.cache.tools.cs2

import dev.openrune.cache.tools.progress.CacheProgress
import dev.openrune.cache.tools.progress.DefaultCacheProgress
import net.lingala.zip4j.ZipFile
import java.io.File

/**
 * Merges a default CS2 install zip into [destRoot].
 *
 * File entries from the zip overwrite the same paths locally. Files that exist only on disk
 * (e.g. under `custom/`, `symbols_custom/`, or extra `.sym` files) are left in place.
 *
 * Directory-only zip entries are not extracted; applying them can replace an existing folder
 * and drop files that are not listed in the archive.
 */
internal object Cs2BundleExtract {

    /** Extracts every file entry, one progress step each: a bundle is close to ten thousand files. */
    fun extract(zipFile: ZipFile, destRoot: File, progress: CacheProgress = DefaultCacheProgress()) {
        destRoot.mkdirs()
        val files = zipFile.fileHeaders.filter { !it.isDirectory }
        val bar = progress.begin("Unpacking CS2 project", files.size.toLong())
        for (header in files) {
            zipFile.extractFile(header, destRoot.absolutePath)
            bar.step()
        }
        bar.close()
    }
}
