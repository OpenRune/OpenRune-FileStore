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
 * Records cache load time and retained memory per definition map, plus the full provider load.
 * Runs on any branch (falls back to `mutableMapOf` where `DenseIntMap` does not exist), so two
 * checkouts can be compared. Skipped unless `-Dbench=true`; cache path via `-DcachePath=`.
 *
 * ```
 * ./gradlew :tools:test --tests dev.openrune.CacheBenchmark -Dbench=true --rerun-tasks
 * ```
 */
@EnabledIfSystemProperty(named = "bench", matches = "true")
class CacheBenchmark {

    private val out = StringBuilder()
    private val cachePath: Path =
        System.getProperty("cachePath")?.let { Path.of(it) } ?: Path.of("..", "data", "cache")

    private fun usedHeap(): Long {
        val runtime = Runtime.getRuntime()
        repeat(5) {
            System.gc()
            Thread.sleep(60)
        }
        return runtime.totalMemory() - runtime.freeMemory()
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> newTarget(): MutableMap<Int, T> = try {
        Class.forName("dev.openrune.cache.DenseIntMap")
            .getDeclaredConstructor(Int::class.javaPrimitiveType)
            .newInstance(16) as MutableMap<Int, T>
    } catch (e: Throwable) {
        mutableMapOf()
    }

    private fun measure(name: String, load: (Cache, MutableMap<Int, Any>) -> Int) {
        val cache = Cache.load(cachePath)
        var count = 0
        val times = LongArray(3) {
            val start = System.nanoTime()
            count = load(cache, newTarget())
            (System.nanoTime() - start) / 1_000_000
        }
        cache.close()

        val before = usedHeap()
        var open: Cache? = Cache.load(cachePath)
        var target: MutableMap<Int, Any>? = newTarget()
        load(open!!, target!!)
        open.close()
        open = null
        val after = usedHeap()

        out.appendLine(
            "%-12s %7d defs  best=%4dms  %6.1f MB  %5d B/def"
                .format(name, count, times.min(), (after - before) / MB, if (count > 0) (after - before) / count else 0)
        )
        target = null
    }

    @Suppress("UNCHECKED_CAST")
    private fun defs(factory: () -> DefinitionDecoder<out Definition>): (Cache, MutableMap<Int, Any>) -> Int =
        { cache, target ->
            runCatching { (factory() as DefinitionDecoder<Definition>).load(cache, target as MutableMap<Int, Definition>) }
            target.size
        }

    @Test
    fun bench() {
        val rev = 240

        run {
            val cache = Cache.load(cachePath)
            var total = 0L
            repeat(3) {
                val start = System.nanoTime()
                val provider = OsrsCacheProvider(cache, rev)
                provider.init()
                val elapsed = (System.nanoTime() - start) / 1_000_000
                if (it == 0 || elapsed < total) total = elapsed
            }
            cache.close()
            out.appendLine("TOTAL OsrsCacheProvider.init best=${total}ms")
        }

        measure("objects", defs { OsrsCacheProvider.ObjectDecoder(rev) })
        measure("items", defs { OsrsCacheProvider.ItemDecoder(rev) })
        measure("npcs", defs { OsrsCacheProvider.NPCDecoder(rev) })
        measure("anims", defs { OsrsCacheProvider.SequenceDecoder(rev) })
        measure("varbits", defs { OsrsCacheProvider.VarBitDecoder() })
        measure("varps", defs { OsrsCacheProvider.VarDecoder() })
        measure("enums", defs { OsrsCacheProvider.EnumDecoder() })
        measure("structs", defs { OsrsCacheProvider.StructDecoder() })
        measure("dbrows", defs { OsrsCacheProvider.DBRowDecoder() })
        measure("sprites", defs { SpriteDecoder() })
        measure("interfaces") { cache, target ->
            @Suppress("UNCHECKED_CAST")
            val components = target as MutableMap<Int, InterfaceType>
            ComponentDecoder(cache, rev).load(components)
            components.values.sumOf { it.components.size }
        }

        File(System.getProperty("java.io.tmpdir"), "openrune-cache-benchmark.txt").writeText(out.toString())
        println(out)
    }

    private companion object {
        const val MB = 1024.0 * 1024.0
    }
}
