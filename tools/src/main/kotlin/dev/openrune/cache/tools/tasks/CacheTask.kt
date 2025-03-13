package dev.openrune.cache.tools.tasks

import dev.openrune.filesystem.Cache

abstract class CacheTask {

    abstract fun init(cache : Cache)

}