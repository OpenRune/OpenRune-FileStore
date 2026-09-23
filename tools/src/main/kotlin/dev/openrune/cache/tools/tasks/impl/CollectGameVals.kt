package dev.openrune.cache.tools.tasks.impl

import com.github.michaelbull.logging.InlineLogger
import dev.openrune.cache.gameval.GameValHandler
import dev.openrune.cache.tools.TaskPriority
import dev.openrune.cache.tools.gameval.GameValPublisher
import dev.openrune.cache.tools.tasks.CacheTask
import dev.openrune.definition.GameValGroupTypes
import dev.openrune.filesystem.Cache

/**
 * Pre-processor that runs before any packer: reads every gameval group already in the cache and seeds
 * [dev.openrune.definition.constants.ConstantProvider] with the keys it does not know yet.
 *
 * Together with [dev.openrune.cache.tools.CacheTool.addGameValMapping], which publishes each gameval
 * as it is packed, this gives every later task one complete view of the ids: the base cache, the
 * project's declared gamevals, and whatever this build has packed so far. Declared gamevals are never
 * overridden here; the cache only fills in what is missing.
 */
internal class CollectGameVals : CacheTask() {

    private val logger = InlineLogger()

    override val priority: TaskPriority
        get() = TaskPriority.GAMEVALS_COLLECT

    override fun init(cache: Cache) {
        if (revision < MIN_REVISION) {
            return
        }

        var seeded = 0
        for (group in GameValGroupTypes.entries) {
            // IFTYPES falls back to IFTYPES_V2 on its own when the old archive is empty.
            if (group == GameValGroupTypes.IFTYPES_V2) continue
            if (group.revision != -1 && revision < group.revision) continue

            val elements = runCatching { GameValHandler.readGameVal(group, cache, revision) }
                .onFailure { failure ->
                    logger.warn(failure) { "CollectGameVals: could not read ${group.name} from the cache" }
                }
                .getOrNull() ?: continue
            elements.forEach { seeded += GameValPublisher.publish(group, it, overwrite = false) }
        }
        logger.debug { "CollectGameVals: seeded $seeded gameval(s) from the cache" }
    }

    private companion object {
        /** Gamevals were introduced to the cache format in this revision. */
        const val MIN_REVISION = 230
    }
}
