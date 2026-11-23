package dev.openrune.cache.tools.tasks.impl.rs2

import dev.openrune.cache.CacheManager
import dev.openrune.cache.tools.FreshCache
import dev.openrune.cache.tools.tasks.CacheTask
import dev.openrune.filesystem.Cache
import dev.openrune.merged.MergedCacheProvider
import dev.openrune.merged.Rs2MergeLayout
import dev.openrune.merged.rs2
import dev.openrune.Rs2Cache
import dev.openrune.rs2.Rs2Content
import dev.openrune.rs2.Rs2Index
import dev.openrune.rs2.Rs2Usage
import dev.openrune.rs2.Rs2Usages
import dev.openrune.rs2.Rs2Use
import java.io.File

object PackCache {

    private const val OSRS_REVISION = 242
    private const val RS2_CACHE_ID = 2747
    private const val RS2_CACHE_BUILD = 951

    private const val OFFSET = Rs2MergeLayout.ID_OFFSET

    private val output = File("build/pack-cache")

    private val configs = listOf(
        Rs2Content.OBJ,
        Rs2Content.NPC,
        Rs2Content.LOC,
        Rs2Content.SEQ,
        Rs2Content.SPOT,
        Rs2Content.ENUM,
        Rs2Content.STRUCT,
        Rs2Content.IDK,
        Rs2Content.PARAM,
        Rs2Content.VAR_BIT,
        Rs2Content.VAR_CLIENT,
        Rs2Content.HITMARK,
        Rs2Content.HEADBAR,
        Rs2Content.FLOOR_OVERLAY,
        Rs2Content.FLOOR_UNDERLAY,
        Rs2Content.LIGHT,
        Rs2Content.BAS,
        Rs2Content.WATER,
        Rs2Content.WORLD_AREA,
        Rs2Content.DBROW,
        Rs2Content.DBTABLE,
    )

    private val archives = listOf(
        Rs2Index.SPRITES,
        Rs2Index.BASES,
        Rs2Index.MAPS,
        Rs2Index.MATERIALS,
        Rs2Index.KEYFRAMES,
        Rs2Index.MODELS_RT7,
        Rs2Index.ANIMS_RT7,
    )

    private val uses = emptyList<Rs2Use>()

    private val extraTasks = emptyList<CacheTask>()

    @JvmStatic
    fun main(args: Array<String>) {
        val usage = scanUsage()
        val filters = buildMap<Int, Set<Int>> {
            usage.sprites.takeIf { it.isNotEmpty() }?.let { put(Rs2Index.SPRITES, it) }
            usage.textures.takeIf { it.isNotEmpty() }?.let { put(Rs2Index.TEXTURES_DXT, it) }
        }

        val source = Rs2Source.local(
            Rs2Staging.stage(
                configs = configs,
                archives = archives,
                rs2CacheId = RS2_CACHE_ID,
                groups = filters,
            ),
        )

        val transfer = rs2Transfer(source) {
            configs.forEach { type ->
                if (type.promoted) {
                    index(from = type.index, key = type.name, label = "RS3 ${type.name}")
                } else {
                    archive(
                        fromIndex = type.index,
                        fromArchive = type.group,
                        toArchive = type.group,
                        key = type.name,
                        label = "RS3 ${type.name}",
                    )
                }
            }
            archives.forEach { from ->
                val wanted = filters[from]
                index(
                    from = from,
                    to = from,
                    archives = if (wanted == null) ({ true }) else ({ it in wanted }),
                    archive = { it + OFFSET },
                    label = "RS3 index $from at +$OFFSET",
                )
            }
        }

        val tasks = mutableListOf<CacheTask>(transfer).also { it += extraTasks }

        FreshCache(cacheOutput = output, tasks = tasks, revision = OSRS_REVISION).initialize()

        decode(transfer.indices)
    }

    private fun scanUsage(): Rs2Usage {
        if (uses.isEmpty()) {
            println("No usage filters, packing every group of $archives")
            return Rs2Usage(emptyMap(), emptySet(), emptySet(), emptySet())
        }
        return Rs2Cache.loadRemote(id = RS2_CACHE_ID).use { rs2 ->
            val usage = Rs2Usages.scan(rs2, uses)
            usage.byUse.forEach { (use, ids) -> println("Usage ${use.name}: ${ids.size}") }
            println("Usage sprites: ${usage.sprites.size} of ${rs2.spriteGroups().size}")
            println("Usage materials: ${usage.materials.size} of ${rs2.materialIds().size}")
            println("Usage textures: ${usage.textures.size}")
            usage.undecoded.forEach { (use, ids) -> println("Usage ${use.name} failed to decode: ${ids.size}") }
            usage
        }
    }

    private fun decode(indices: Map<String, Int>) {
        val cache = Cache.load(output.toPath())
        try {
            val provider = MergedCacheProvider(
                cache,
                rs2Build = RS2_CACHE_BUILD,
                osrsRevision = OSRS_REVISION,
                indices = indices,
            )
            CacheManager.init(provider)

            println("${provider.rs2Total()} RS3 definitions, offset $OFFSET")
            println("  item: OSRS ${CacheManager.getItem(995)?.name} | RS3 ${CacheManager.getItem(OFFSET + 995)?.name}")
            println("  npc:  OSRS ${CacheManager.getNpc(1)?.name} | RS3 ${CacheManager.getNpc(OFFSET + 1)?.name}")
            println("  npc ${OFFSET + 1} keeps RS3 id ${CacheManager.getNpc(OFFSET + 1)?.rs2?.id}")

            archives.forEach { index ->
                val osrs = cache.archives(index).count { it < OFFSET }
                val rs2 = cache.archives(index).count { it >= OFFSET }
                println("  index $index: $osrs OSRS groups, $rs2 RS3 groups at +$OFFSET")
            }
        } finally {
            cache.close()
        }
    }
}
