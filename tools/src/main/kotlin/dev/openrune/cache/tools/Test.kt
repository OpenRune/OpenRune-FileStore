package dev.openrune.cache.tools


import com.displee.cache.CacheLibrary
import dev.openrune.cache.CacheDelegate

fun main() {
    val cachePath = System.getProperty("cachePath") ?: "D:/RSPS/Fluxious/Flux-Server/.data/cache/LIVE"

    val cache = CacheDelegate(CacheLibrary(cachePath))

    WorldMapPacker(cache).repack()
}
