package dev.openrune

import dev.openrune.cache.filestore.definition.ComponentDecoder
import dev.openrune.cache.filestore.definition.DefinitionDecoder
import dev.openrune.cache.filestore.definition.InterfaceType
import dev.openrune.cache.filestore.definition.SpriteDecoder
import dev.openrune.definition.Definition
import dev.openrune.filesystem.Cache
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfSystemProperty
import java.io.File
import java.nio.file.Path

/**
 * Retained-heap harness for the definition types, used to produce the memory figures in
 * `PERFORMANCE.md`. Skipped unless `-Dbench=true` is set.
 *
 * ```
 * ./gradlew :tools:test --tests dev.openrune.CacheMemoryBench -Dbench=true --rerun-tasks
 * ```
 *
 * Each figure is the settled heap delta from loading one definition type and holding only its map.
 * Results are written to `openrune-memory-bench.txt` in the temp directory so two checkouts can be
 * diffed.
 */
@EnabledIfSystemProperty(named = "bench", matches = "true")
class CacheMemoryBench {

    private val out = StringBuilder()

    private fun usedHeap(): Long {
        val runtime = Runtime.getRuntime()
        repeat(5) {
            System.gc()
            Thread.sleep(60)
        }
        return runtime.totalMemory() - runtime.freeMemory()
    }

    private fun measure(name: String, load: (Cache) -> Pair<Int, Any>) {
        // A fresh cache per type so its internal caches do not carry between measurements. The
        // baseline is taken before the cache is opened and the cache is released before the end
        // figure, so the delta is the definitions alone.
        val before = usedHeap()
        var cache: Cache? = Cache.load(Path.of("..", "data", "cache"))
        var loaded: Any? = load(cache!!)
        val count = (loaded as Pair<*, *>).first as Int
        cache.close()
        cache = null
        val after = usedHeap()

        val delta = after - before
        val each = if (count > 0) delta / count else 0
        out.appendLine("%-14s %7d defs  %6.1f MB  %5d B/def".format(name, count, delta / MB, each))
        loaded = null
    }

    private fun <T : Definition> defs(factory: () -> DefinitionDecoder<T>): (Cache) -> Pair<Int, Any> = { cache ->
        val target = HashMap<Int, T>()
        runCatching { factory().load(cache, target) }
        target.size to target
    }

    @Test
    fun bench() {
        val rev = 240

        out.appendLine("== retained heap per definition type ==")
        measure("objects", defs { OsrsCacheProvider.ObjectDecoder(rev) })
        measure("items", defs { OsrsCacheProvider.ItemDecoder(rev) })
        measure("npcs", defs { OsrsCacheProvider.NPCDecoder(rev) })
        measure("anims", defs { OsrsCacheProvider.SequenceDecoder(rev) })
        measure("varbits", defs { OsrsCacheProvider.VarBitDecoder() })
        measure("enums", defs { OsrsCacheProvider.EnumDecoder() })
        measure("structs", defs { OsrsCacheProvider.StructDecoder() })
        measure("dbrows", defs { OsrsCacheProvider.DBRowDecoder() })
        measure("sprites", defs { SpriteDecoder() })
        measure("interfaces") { cache ->
            val components = HashMap<Int, InterfaceType>()
            ComponentDecoder(cache, rev).load(components)
            components.values.sumOf { it.components.size } to components
        }

        File(System.getProperty("java.io.tmpdir"), "openrune-memory-bench.txt").writeText(out.toString())
        println(out)
    }

    private companion object {
        const val MB = 1024.0 * 1024.0
    }
}
