package dev.openrune.cache.tools.worldmap.packing

import dev.openrune.OsrsCacheProvider
import dev.openrune.cache.WORLDMAPAREAS
import dev.openrune.cache.tools.Builder
import dev.openrune.cache.tools.tasks.TaskType
import dev.openrune.cache.tools.worldmap.MapsquareMultiSection
import dev.openrune.cache.tools.worldmap.MapsquareSingleSection
import dev.openrune.cache.tools.worldmap.WorldMapAreaDetails
import dev.openrune.cache.tools.worldmap.WorldMapSection
import dev.openrune.cache.tools.worldmap.ZoneMultiSection
import dev.openrune.cache.tools.worldmap.ZoneSingleSection
import dev.openrune.cache.tools.worldmap.packing.worldmap.WorldMapAreaBlock
import dev.openrune.definition.type.WorldMapAreaType
import dev.openrune.filesystem.Cache
import io.netty.buffer.Unpooled
import kotlinx.serialization.encodeToString
import net.peanuuutz.tomlkt.Toml
import java.io.File
import kotlin.io.path.Path

fun main() {

//    val builder = Builder(
//        type = TaskType.FRESH_INSTALL,
//        cacheLocation = File("C:\\Users\\chris\\Desktop\\225 Cache")
//    )
//    builder.revision(225)
//    builder.removeXteas()
//    builder.build().initialize()
//
    val cache = Cache.load(Path("D:\\RSPS\\Fluxious\\Flux-Server\\.data\\cache\\LIVE"))
//
//
//    cache.archives(WORLDMAPAREAS).forEach { archive ->
//        cache.files(WORLDMAPAREAS, archive).forEach { id ->
//
//            val data = cache.data(WORLDMAPAREAS, archive, id)
//                ?: return@forEach
//
//            val block = WorldMapAreaBlock(
//                details = WorldMapAreaDetails.decode(
//                    id,
//                    Unpooled.wrappedBuffer(data)
//                ),
//                mapElements = emptyList()
//            )
//
//            println(block.toToml())
//        }
//    }



    //val wma = mutableMapOf<Int, WorldMapAreaType>()
    //OsrsCacheProvider.WorldMapAreasDecoder(225).load(cache,wma)
    //wma.forEach {
      //  println(it.value)
    //}
    val mapPacker = WorldMapPacker(cache)
    mapPacker.pack(Path("D:\\RSPS\\Fluxious\\Flux-Server\\.data\\cache\\LIVE"))
}

fun WorldMapAreaBlock.toToml(): String = buildString {
    appendLine("[details]")
    appendLine("id = ${details.id}")
    appendLine("internalName = \"${details.internalName}\"")
    appendLine("displayName = \"${details.displayName}\"")
    appendLine("backgroundColour = ${details.backgroundColour}")
    appendLine("zoom = ${details.zoom}")
    appendLine("isMain = ${details.isMain}")
    appendLine()

    appendLine("[details.origin]")
    appendLine("x = ${details.origin.x}")
    appendLine("y = ${details.origin.y}")
    appendLine("plane = ${details.origin.level}")
    appendLine()

    details.sections.forEachIndexed { i, s ->
        appendLine("[[details.sections]]")
        appendLine("type = \"${s.type}\"")
        appendLine("level = ${s.level}")
        appendLine("levelsCount = ${s.levelsCount}")

        when (s) {
            is MapsquareSingleSection -> {
                appendLine("mapsquareSourceX = ${s.mapsquareSourceX}")
                appendLine("mapsquareSourceY = ${s.mapsquareSourceY}")
                appendLine("mapsquareDestinationX = ${s.mapsquareDestinationX}")
                appendLine("mapsquareDestinationY = ${s.mapsquareDestinationY}")
            }

            is ZoneSingleSection -> {
                appendLine("mapsquareSourceX = ${s.mapsquareSourceX}")
                appendLine("zoneSourceX = ${s.zoneSourceX}")
                appendLine("mapsquareSourceY = ${s.mapsquareSourceY}")
                appendLine("zoneSourceY = ${s.zoneSourceY}")
            }

            is MapsquareMultiSection -> {
                appendLine("sourceMinX = ${s.mapsquareSourceMinX}")
                appendLine("sourceMaxX = ${s.mapsquareSourceMaxX}")
            }

            is ZoneMultiSection -> {
                appendLine("mapsquareSourceX = ${s.mapsquareSourceX}")
                appendLine("zoneSourceMinX = ${s.zoneSourceMinX}")
                appendLine("zoneSourceMaxX = ${s.zoneSourceMaxX}")
            }
        }

        appendLine()
    }
}

private fun String.toml(): String {
    return "\"" + replace("\"", "\\\"") + "\""
}