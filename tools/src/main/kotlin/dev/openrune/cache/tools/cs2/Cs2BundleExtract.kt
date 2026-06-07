package dev.openrune.cache.tools.cs2

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

    fun extract(zipFile: ZipFile, destRoot: File) {
        destRoot.mkdirs()
        for (header in zipFile.fileHeaders) {
            if (header.isDirectory) continue
            zipFile.extractFile(header, destRoot.absolutePath)
        }
    }
}
