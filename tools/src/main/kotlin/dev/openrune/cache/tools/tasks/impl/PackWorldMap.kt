package dev.openrune.cache.tools.tasks.impl

import dev.openrune.cache.tools.TaskPriority
import dev.openrune.cache.tools.WorldMapPacker
import dev.openrune.cache.tools.incremental.PackedMapSquares
import dev.openrune.cache.tools.tasks.CacheTask
import dev.openrune.cache.util.logger
import dev.openrune.filesystem.Cache
import java.nio.file.Path

/**
 * Regenerates and re-encodes world map areas after the maps themselves have been packed.
 *
 * By default only the areas that source a mapsquare packed during this build are rebuilt, so an
 * incremental build that touched a handful of squares does not pay for all 50-odd areas. Set
 * [all] to rebuild every area regardless.
 *
 * Runs at [TaskPriority.END] so [PackMaps] has already recorded which squares changed.
 */
class PackWorldMap(
    private val all: Boolean = false,
    private val imageOutputDir: Path? = null,
) : CacheTask() {

    override val priority: TaskPriority = TaskPriority.END

    override fun init(cache: Cache) {
        val packer = WorldMapPacker(cache)
        if (all) {
            packer.repack(imageOutputDir)
            return
        }
        val changed = PackedMapSquares.packed
        if (changed.isEmpty()) {
            logger.info { "No mapsquares were packed this build; skipping world map." }
            return
        }
        packer.repackChanged(changed, imageOutputDir)
    }
}
