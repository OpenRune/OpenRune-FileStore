package dev.openrune.cache.tools.tasks.impl

import dev.openrune.cache.tools.TaskPriority
import dev.openrune.cache.tools.WorldMapPacker
import dev.openrune.cache.tools.tasks.CacheTask
import dev.openrune.cache.util.logger
import dev.openrune.filesystem.Cache
import java.io.File
import java.nio.file.Path

/**
 * Renders every world map area in the cache to a PNG in [outputDir].
 *
 * Read-only: nothing is written back to the cache, so this is safe to leave in a build. It runs at
 * [TaskPriority.END] so the images show the cache as the build left it, world map included.
 *
 * [pixelsPerTile] is the render scale; 4 matches what the client draws at full zoom, and 1 produces a
 * minimap-sized image roughly a sixteenth the size.
 */
class DumpWorldMap(
    private val outputDir: Path,
    private val pixelsPerTile: Int = 4,
) : CacheTask() {

    constructor(outputDir: File, pixelsPerTile: Int = 4) : this(outputDir.toPath(), pixelsPerTile)

    override val priority: TaskPriority = TaskPriority.END

    override fun init(cache: Cache) {
        require(pixelsPerTile > 0) { "pixelsPerTile must be positive, was $pixelsPerTile" }
        logger.info { "Dumping world map areas to ${outputDir.toAbsolutePath()}" }
        WorldMapPacker(cache, progress).dumpImages(outputDir, pixelsPerTile)
    }
}
