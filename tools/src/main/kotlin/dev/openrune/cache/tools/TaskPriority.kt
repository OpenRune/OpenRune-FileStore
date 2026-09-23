package dev.openrune.cache.tools

/**
 * Stage a [dev.openrune.cache.tools.tasks.CacheTask] runs in. Tasks are stably sorted by
 * [priorityValue], so tasks sharing a stage keep the order they were added in.
 *
 * The stages exist because later packers reference what earlier ones produce:
 *
 * 1. [GAMEVALS_COLLECT] – seed the constant provider with every gameval already in the cache.
 * 2. [INTERFACES] – pack interfaces; publishes `interface.*` / `component.*` ids.
 * 3. [DBTABLES] – pack db tables; publishes `dbtable.*` / `dbcol.*` / `dbrow.*` ids.
 * 4. [NORMAL] – configs, sprites, models and anything else that may reference the above.
 * 5. [END] – tasks that read the packed configs (auto-cert, ...).
 * 6. [VERY_LAST] – server-only config packing.
 * 7. [MAPS] – map squares and the world map derived from them.
 * 8. [GAMEVALS_ENCODE] – write every collected gameval into the cache.
 * 9. [CS2] – unpack and compile scripts; runs after the gamevals are encoded because its
 *    symbol dump reads them back out of the cache.
 */
enum class TaskPriority(val priorityValue: Int) {
    GAMEVALS_COLLECT(-30),
    INTERFACES(-20),
    DBTABLES(-10),
    NORMAL(0),
    END(10),
    VERY_LAST(20),
    MAPS(30),
    GAMEVALS_ENCODE(40),
    CS2(50),
    ;

    companion object {
        @Deprecated("Renamed to CS2", ReplaceWith("TaskPriority.CS2"))
        val CS2_LAST: TaskPriority get() = CS2
    }
}
