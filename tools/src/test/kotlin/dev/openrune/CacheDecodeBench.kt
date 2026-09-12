package dev.openrune

import dev.openrune.cache.CONFIGS
import dev.openrune.cache.DBROW
import dev.openrune.cache.ENUM
import dev.openrune.cache.ITEM
import dev.openrune.cache.NPC
import dev.openrune.cache.OBJECT
import dev.openrune.cache.SPRITES
import dev.openrune.cache.filestore.definition.ComponentDecoder
import dev.openrune.cache.filestore.definition.DefinitionDecoder
import dev.openrune.cache.filestore.definition.FontDecoder
import dev.openrune.cache.filestore.definition.InterfaceType
import dev.openrune.cache.filestore.definition.SpriteDecoder
import dev.openrune.cache.gameval.GameValHandler
import dev.openrune.definition.Definition
import dev.openrune.definition.DefinitionCodec
import dev.openrune.definition.GameValGroupTypes
import dev.openrune.definition.codec.DBRowCodec
import dev.openrune.definition.codec.EnumCodec
import dev.openrune.definition.codec.ItemCodec
import dev.openrune.definition.codec.NPCCodec
import dev.openrune.definition.codec.ObjectCodec
import dev.openrune.definition.codec.SpriteCodec
import dev.openrune.definition.type.ItemType
import dev.openrune.definition.type.NpcType
import dev.openrune.definition.type.ObjectType
import dev.openrune.definition.type.SpriteType
import dev.openrune.filesystem.Cache
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfSystemProperty
import java.io.File
import java.nio.file.Path

/**
 * Timing harness for the cache read path, used to produce the figures in `PERFORMANCE.md`.
 *
 * Skipped unless `-Dbench=true` is set: it needs a cache in `data/cache` and takes about a minute.
 *
 * ```
 * ./gradlew :tools:test --tests dev.openrune.CacheDecodeBench -Dbench=true --rerun-tasks
 * ```
 *
 * Results are printed and written to `openrune-decode-bench.txt` in the temp directory, so two
 * checkouts can be diffed. Each figure is the best of several in-process runs.
 *
 * The end-to-end figures include `Cache.data`, which dominates them. The "codec only" section
 * pre-fetches every payload and times the decode alone.
 */
@EnabledIfSystemProperty(named = "bench", matches = "true")
class CacheDecodeBench {

    private companion object {
        const val MB = 1024L * 1024L
    }

    private val out = StringBuilder()

    /** Settles the heap as far as a test can, so the figure reflects what is actually retained. */
    private fun usedHeap(): Long {
        val runtime = Runtime.getRuntime()
        repeat(4) {
            System.gc()
            Thread.sleep(60)
        }
        return runtime.totalMemory() - runtime.freeMemory()
    }

    private fun measure(name: String, runs: Int, body: () -> Int) {
        val times = mutableListOf<Long>()
        var count = 0
        repeat(runs) {
            val start = System.nanoTime()
            count = body()
            times += (System.nanoTime() - start) / 1_000_000
        }
        out.appendLine("%-24s best=%5dms  n=%-7d all=%s".format(name, times.min(), count, times))
    }

    private fun <T : Definition> decoder(name: String, cache: Cache, factory: () -> DefinitionDecoder<T>) =
        measure(name, 4) {
            val target = mutableMapOf<Int, T>()
            runCatching { factory().load(cache, target) }
            target.size
        }

    /** Times the codec alone, over payloads already pulled out of the cache. */
    private fun <T : Definition> codecOnly(
        name: String,
        cache: Cache,
        ids: IntArray,
        fetch: (Int) -> ByteArray?,
        factory: () -> DefinitionCodec<T>,
    ) {
        val payloads = arrayOfNulls<ByteArray>(ids.size)
        for (i in ids.indices) payloads[i] = fetch(ids[i])
        measure(name, 5) {
            val codec = factory()
            var decoded = 0
            for (i in ids.indices) {
                val data = payloads[i] ?: continue
                runCatching { codec.loadData(ids[i], data) }.onSuccess { decoded++ }
            }
            decoded
        }
    }

    @Test
    fun bench() {
        val cache = Cache.load(Path.of("..", "data", "cache"))
        val rev = 240

        out.appendLine("== end to end (includes Cache.data i/o) ==")

        measure("TOTAL definitions", 5) {
            val provider = OsrsCacheProvider(cache, rev)
            provider.init()
            provider.objects.size
        }

        decoder("  objects", cache) { OsrsCacheProvider.ObjectDecoder(rev) }
        decoder("  npcs", cache) { OsrsCacheProvider.NPCDecoder(rev) }
        decoder("  items", cache) { OsrsCacheProvider.ItemDecoder(rev) }
        decoder("  anims", cache) { OsrsCacheProvider.SequenceDecoder(rev) }
        decoder("  varbits", cache) { OsrsCacheProvider.VarBitDecoder() }
        decoder("  varps", cache) { OsrsCacheProvider.VarDecoder() }
        decoder("  enums", cache) { OsrsCacheProvider.EnumDecoder() }
        decoder("  structs", cache) { OsrsCacheProvider.StructDecoder() }
        decoder("  dbrows", cache) { OsrsCacheProvider.DBRowDecoder() }
        decoder("  dbtables", cache) { OsrsCacheProvider.DBTableDecoder() }

        measure("gamevals (all groups)", 3) {
            var total = 0
            for (group in GameValGroupTypes.entries) {
                total += runCatching { GameValHandler.readGameVal(group, cache, rev).size }.getOrDefault(0)
            }
            total
        }

        measure("interfaces", 3) {
            val components = mutableMapOf<Int, InterfaceType>()
            ComponentDecoder(cache, rev).load(components)
            components.values.sumOf { it.components.size }
        }

        measure("sprites", 3) {
            val sprites = mutableMapOf<Int, SpriteType>()
            runCatching { SpriteDecoder().load(cache, sprites) }
            sprites.size
        }

        measure("fonts", 3) {
            runCatching { FontDecoder(cache).loadAllFonts().size }.getOrDefault(0)
        }

        out.appendLine()
        out.appendLine("== codec only (payloads pre-fetched, no Cache.data in the timed section) ==")

        codecOnly("objects", cache, cache.files(CONFIGS, OBJECT), { cache.data(CONFIGS, OBJECT, it) }) { ObjectCodec(rev) }
        codecOnly("npcs", cache, cache.files(CONFIGS, NPC), { cache.data(CONFIGS, NPC, it) }) { NPCCodec(rev) }
        codecOnly("items", cache, cache.files(CONFIGS, ITEM), { cache.data(CONFIGS, ITEM, it) }) { ItemCodec(rev) }
        codecOnly("dbrows", cache, cache.files(CONFIGS, DBROW), { cache.data(CONFIGS, DBROW, it) }) { DBRowCodec() }
        codecOnly("enums", cache, cache.files(CONFIGS, ENUM), { cache.data(CONFIGS, ENUM, it) }) { EnumCodec() }
        codecOnly("sprites", cache, cache.archives(SPRITES), { cache.data(SPRITES, it, 0) }) { SpriteCodec() }

        out.appendLine()
        out.appendLine("== retained heap ==")
        run {
            // Decoders are used directly rather than OsrsCacheProvider, which holds the cache and
            // would keep it reachable no matter what this does with its own reference.
            var open: Cache? = Cache.load(Path.of("..", "data", "cache"))
            val afterOpen = usedHeap()

            val objects = mutableMapOf<Int, ObjectType>()
            val items = mutableMapOf<Int, ItemType>()
            val npcs = mutableMapOf<Int, NpcType>()
            OsrsCacheProvider.ObjectDecoder(rev).load(open!!, objects)
            OsrsCacheProvider.ItemDecoder(rev).load(open!!, items)
            OsrsCacheProvider.NPCDecoder(rev).load(open!!, npcs)
            val afterLoad = usedHeap()

            open!!.close()
            open = null
            val afterRelease = usedHeap()

            out.appendLine("%-26s %5d MB".format("cache open", afterOpen / MB))
            out.appendLine("%-26s %5d MB".format("definitions loaded", afterLoad / MB))
            out.appendLine("%-26s %5d MB".format("cache released", afterRelease / MB))
            out.appendLine("%-26s %5d MB".format("  held by the cache", (afterLoad - afterRelease) / MB))
            out.appendLine("%-26s %5d MB".format("  held by definitions", afterRelease / MB))
            out.appendLine("%-26s %5d".format("objects retained", objects.size + items.size + npcs.size))
        }

        out.appendLine()
        out.appendLine("== Cache.data i/o alone ==")
        val objectIds = cache.files(CONFIGS, OBJECT)
        measure("objects: cache fetch", 4) {
            var bytes = 0
            for (id in objectIds) bytes += cache.data(CONFIGS, OBJECT, id)?.size ?: 0
            objectIds.size
        }

        File(System.getProperty("java.io.tmpdir"), "openrune-decode-bench.txt").writeText(out.toString())
        println(out)
    }
}

