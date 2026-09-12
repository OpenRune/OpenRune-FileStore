package dev.openrune.cache.tools

import com.displee.cache.CacheLibrary
import dev.openrune.cache.CacheDelegate
import kotlin.io.path.Path

/** Renders every world map area in a cache to PNG. Read-only. */
fun main() {
    val path = System.getProperty("cachePath") ?: "D:/RSPS/Fluxious/Flux-Server/.data/cache/LIVE"
    val out = System.getProperty("out") ?: "D:/OpenRune/OpenRune-FileStore/worldmap-out/dump"
    WorldMapPacker(CacheDelegate(CacheLibrary(path))).dumpImages(Path(out))
}
