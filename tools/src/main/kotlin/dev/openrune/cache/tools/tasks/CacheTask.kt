package dev.openrune.cache.tools.tasks

import dev.openrune.cache.tools.TaskPriority
import dev.openrune.filesystem.Cache

abstract class CacheTask(val serverTaskOnly : Boolean = false) {
    /**
     * Cache build revision, assigned by [dev.openrune.cache.tools.BuildCache] before [init] runs.
     * Must not remain `-1` when CS2 tasks execute; use a resolved cache revision (e.g. from `version.dat`).
     */
    open var revision: Int = -1
    internal var serverPass : Boolean = false
    open val priority: TaskPriority = TaskPriority.NORMAL
    abstract fun init(cache : Cache)

    companion object {
        /** Minimum cache revision supported by [dev.openrune.cache.tools.cs2.PackCs2] / [dev.openrune.cache.tools.cs2.UnpackDefaultCs2]. */
        const val CS2_MIN_CACHE_REVISION: Int = 230
    }
}