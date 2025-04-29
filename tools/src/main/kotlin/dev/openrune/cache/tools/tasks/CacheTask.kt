package dev.openrune.cache.tools.tasks

import dev.openrune.cache.tools.TaskPriority
import dev.openrune.filesystem.Cache

abstract class CacheTask {

    open val priority: TaskPriority = TaskPriority.NORMAL
    abstract fun init(cache : Cache)

}