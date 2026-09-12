package dev.openrune.cache.tools.gameval

import com.github.michaelbull.logging.InlineLogger
import dev.openrune.cache.tools.autocert.AutoCertSettings
import dev.openrune.cache.tools.autocert.CertCandidate
import dev.openrune.definition.constants.ConstantProvider
import dev.openrune.definition.constants.GameValWrite
import dev.openrune.definition.constants.MutableMappingProvider

object GameValAssigner {

    const val MAX_ID: Int = 65535
    const val RESERVED_TOP_IDS: Int = 10
    private const val UNASSIGNED = -1

    val TABLE_MAX_IDS: Map<String, Int> = mapOf("obj" to 63487, "interface" to 10000)

    private val logger = InlineLogger()

    data class Result(
        val assigned: List<GameValWrite>,
        val certIds: Map<String, Int>,
    ) {
        val isEmpty: Boolean get() = assigned.isEmpty()
    }

    fun assign(
        candidates: List<CertCandidate> = emptyList(),
        settings: AutoCertSettings? = null,
        maxId: Int = MAX_ID,
    ): Result {
        val providers = ConstantProvider.loadedProviders().filterIsInstance<MutableMappingProvider>()
        if (providers.isEmpty()) {
            if (candidates.isNotEmpty()) {
                logger.warn {
                    "Auto-cert is on but no loaded mapping provider can be written to, so no cert " +
                        "gamevals can be reserved. The provider needs to implement MutableMappingProvider."
                }
            }
            return Result(emptyList(), emptyMap())
        }

        val certTable = settings?.table ?: "obj"
        val certByItem = candidates.associateBy { it.itemKey }
        val used = usedIdsByTable()
        val floors = mutableMapOf<String, Int>()
        val writes = mutableListOf<Pair<MutableMappingProvider, GameValWrite>>()
        val certIds = linkedMapOf<String, Int>()
        val certOwner = mutableMapOf<String, MutableMappingProvider>()

        fun floorFor(table: String): Int =
            floors.getOrPut(table) { providers.maxOf { it.maxBaseId(table) } + 1 }

        fun ceilingFor(table: String): Int =
            (TABLE_MAX_IDS[table] ?: maxId) - RESERVED_TOP_IDS

        val seen = mutableSetOf<Pair<String, String>>()
        for (provider in providers) {
            for (placeholder in provider.unassignedGameVals()) {
                if (!seen.add(placeholder.table to placeholder.key)) continue

                val table = placeholder.table
                val pool = used.getOrPut(table) { mutableSetOf() }
                val floor = floorFor(table)
                val candidate =
                    if (table == certTable) certByItem[placeholder.key] else null

                val ceiling = ceilingFor(table)
                val id = if (candidate != null) {
                    nextFreePair(pool, floor, ceiling, table)
                } else {
                    nextFree(pool, floor, ceiling, table)
                }
                pool += id
                writes += provider to GameValWrite(table, placeholder.key, id, placeholder.source)

                if (candidate != null) {
                    val certId = id + 1
                    pool += certId
                    certIds[candidate.certKey] = certId
                    certOwner[candidate.certKey] = provider
                }
            }
        }

        for (candidate in candidates) {
            if (certIds.containsKey(candidate.certKey)) continue

            val itemId = ConstantProvider.getMappingOrNull("$certTable.${candidate.itemKey}")
            if (itemId == null || itemId == UNASSIGNED) {
                logger.warn {
                    "Auto-cert skipped '${candidate.itemKey}': it has no gameval id yet. Declare it " +
                        "as `${candidate.itemKey} = -1` so an id can be reserved."
                }
                continue
            }

            val owner = providers.firstOrNull { it.sourceOf(certTable, candidate.itemKey) != null }
                ?: providers.first()
            certOwner[candidate.certKey] = owner

            val existing = ConstantProvider.getMappingOrNull("$certTable.${candidate.certKey}")
            if (existing != null && existing != UNASSIGNED) {
                certIds[candidate.certKey] = existing
                continue
            }

            val ceiling = ceilingFor(certTable)
            if (itemId + 1 > ceiling) {
                logger.warn {
                    "Auto-cert skipped '${candidate.itemKey}' ($itemId): the last " +
                        "usable ids for '$certTable' end at $ceiling, so there is no room for a " +
                        "cert above it. Move the item below $ceiling to cert it."
                }
                certOwner.remove(candidate.certKey)
                continue
            }

            val pool = used.getOrPut(certTable) { mutableSetOf() }
            val adjacent = itemId + 1
            val certId = when {
                adjacent !in pool && adjacent <= ceiling -> adjacent
                settings?.requireAdjacentIds != false -> error(
                    "Auto-cert cannot place '${candidate.certKey}' at ${itemId + 1}: that id is " +
                        "already taken and a cert must sit one above its item. Free the id, move " +
                        "'${candidate.itemKey}' by setting it back to -1, or set " +
                        "requireAdjacentIds = false."
                )
                else -> nextFree(pool, floorFor(certTable), ceiling, certTable)
            }

            pool += certId
            certIds[candidate.certKey] = certId
        }

        certIds.forEach { (certKey, certId) ->
            val owner = certOwner[certKey] ?: providers.first()
            writes += owner to GameValWrite(
                table = certTable,
                key = certKey,
                id = certId,
                after = candidates.firstOrNull { it.certKey == certKey }?.itemKey,
                generated = true,
            )
        }

        if (writes.isEmpty()) return Result(emptyList(), certIds)

        writes.groupBy({ it.first }, { it.second }).forEach { (provider, entries) ->
            provider.writeGameVals(entries)
        }
        writes.forEach { (_, entry) -> ConstantProvider.putMapping(entry.table, entry.key, entry.id) }

        val assigned = writes.map { it.second }
        logger.info {
            "Assigned ${assigned.count { !it.generated }} gameval id(s)" +
                if (certIds.isEmpty()) "" else ", ${certIds.size} auto-cert"
        }
        return Result(assigned, certIds)
    }

    private fun usedIdsByTable(): MutableMap<String, MutableSet<Int>> {
        val used = mutableMapOf<String, MutableSet<Int>>()
        ConstantProvider.mappings.forEach { (table, entries) ->
            used[table] = entries.values.filterTo(mutableSetOf()) { it != UNASSIGNED }
        }
        return used
    }

    private fun nextFree(used: Set<Int>, floor: Int, ceiling: Int, table: String): Int =
        (ceiling downTo floor).firstOrNull { it !in used }
            ?: error("No free gameval id left for table '$table' in [$floor..$ceiling]")

    private fun nextFreePair(used: Set<Int>, floor: Int, ceiling: Int, table: String): Int =
        (ceiling - 1 downTo floor).firstOrNull { it !in used && (it + 1) !in used }
            ?: error(
                "No free adjacent gameval id pair left for table '$table' in [$floor..$ceiling]; " +
                    "auto-cert needs an item and its cert to be one apart"
            )
}
