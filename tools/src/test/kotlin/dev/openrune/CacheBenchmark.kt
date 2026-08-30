package dev.openrune

import dev.openrune.cache.CacheManager
import dev.openrune.filesystem.Cache
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Path

/**
 * Temporary benchmark harness used while profiling cache load/decode.
 * Writes results to build/bench-result.txt so they survive Gradle's test log filtering.
 */
class CacheBenchmark {

    private fun used(): Long {
        repeat(4) {
            System.gc()
            Thread.sleep(120)
        }
        val rt = Runtime.getRuntime()
        return rt.totalMemory() - rt.freeMemory()
    }

    @Test
    fun bench() {
        val path = Path.of("..", "data", "cache")
        val out = StringBuilder()

        val base = used()
        var t = System.nanoTime()
        val cache = Cache.load(path)
        val loadMs = (System.nanoTime() - t) / 1_000_000
        val afterLoad = used()
        out.appendLine("index load : ${loadMs}ms, heap +${(afterLoad - base) / 1024 / 1024}MB")

        // Best of several in-process runs: the first is dominated by class loading and JIT warm-up.
        val decodeTimes = mutableListOf<Long>()
        var afterDecode = afterLoad
        repeat(5) { iteration ->
            t = System.nanoTime()
            val provider = OsrsCacheProvider(cache, 240)
            provider.init()
            decodeTimes += (System.nanoTime() - t) / 1_000_000
            if (iteration == 0) {
                CacheManager.init(provider)
                afterDecode = used()
            }
        }
        out.appendLine("definitions: ${decodeTimes.min()}ms best of ${decodeTimes}, heap +${(afterDecode - afterLoad) / 1024 / 1024}MB")
        out.appendLine("counts     : items=${CacheManager.itemSize()} npcs=${CacheManager.npcSize()} objs=${CacheManager.objectSize()}")

        // ComponentDecoder's gameval name lookup, isolated from the (revision-sensitive) component decode.
        val lookupTimes = mutableListOf<Long>()
        var names = 0
        repeat(3) {
            t = System.nanoTime()
            val gamevals = dev.openrune.cache.gameval.GameValHandler.readGameVal(
                dev.openrune.definition.GameValGroupTypes.IFTYPES, cache, 240
            )
            names = 0
            for (group in cache.archives(dev.openrune.cache.INTERFACES)) {
                for (file in cache.files(dev.openrune.cache.INTERFACES, group)) {
                    val combinedId = (group shl 16) or file
                    val interfaceId = (combinedId ushr 16) and 0xFFFF
                    val childIdRaw = combinedId and 0xFFFF
                    val name = with(dev.openrune.cache.gameval.GameValHandler) {
                        gamevals.lookupAs<dev.openrune.cache.gameval.impl.Interface>(interfaceId)
                            ?.components?.lookup(childIdRaw)?.name
                    }
                    if (name != null) names++
                }
            }
            lookupTimes += (System.nanoTime() - t) / 1_000_000
        }
        out.appendLine("gameval lkp: ${lookupTimes.min()}ms best of ${lookupTimes} ($names named components)")

        File("build").mkdirs()
        File("build/bench-result.txt").writeText(out.toString())
        println(out)
    }
}
