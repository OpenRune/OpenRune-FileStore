@file:Suppress("unused")

package dev.openrune.cache.tools.worldmap.builder.test

import dev.openrune.cache.tools.worldmap.*
import dev.openrune.cache.tools.worldmap.builder.blocks.WorldMapMultiMapsquareBuilder
import dev.openrune.cache.tools.worldmap.builder.blocks.WorldMapMultiZoneBuilder
import dev.openrune.cache.tools.worldmap.builder.blocks.WorldMapSingleMapsquareBuilder
import dev.openrune.cache.tools.worldmap.builder.blocks.WorldMapSingleZoneBuilder
import dev.openrune.cache.tools.worldmap.config.WorldMapConfig
import dev.openrune.cache.tools.worldmap.ground.MapsquareGround
import dev.openrune.cache.tools.worldmap.ground.MapsquareId
import dev.openrune.cache.tools.worldmap.providers.*
import dev.openrune.filesystem.Cache
import io.netty.buffer.Unpooled
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import javax.imageio.ImageIO

/**
 * @author Kris | 18/08/2022
 */
class WorldMap(private val config: WorldMapConfig, val cache: Cache, val cache2 : org.openrs2.cache.Cache) {

    fun generateImage(
        providers: Providers,
        details: WorldMapAreaDetails,
        labels: List<WorldMapElement>,
        pixelsPerTile: Int,
        bordersSeparate: Boolean = config.blendBordersSeparately,
    ): BufferedImage {
        val (mapsquares, zones) = generateBlocks(
            details,
            providers.mapProvider,
            providers.objectProvider
        )
        val newArea = WorldMapArea("", details, WorldMapAreaData(mapsquares, zones, labels))
        return MapsquareGround.generateMapImage(
            providers,
            newArea,
            bordersSeparate,
            pixelsPerTile,
            config.brightness,
            generateUnderlays = true
        )
    }

    fun generateImageFromExistingData(
        id : Int,
        rscmName: String,
        providers: Providers,
        pixelsPerTile: Int,
        bordersSeparate: Boolean = config.blendBordersSeparately,
    ): BufferedImage {
        return MapsquareGround.generateMapImage(
            providers,
            WorldMapArea.decode(providers.cacheProvider, id,rscmName),
            bordersSeparate,
            pixelsPerTile,
            config.brightness,
            generateUnderlays = false
        )
    }

    fun exists(providers: Providers, internalName: String): Boolean {
        return providers.cacheProvider.exists(WORLD_MAP_DATA_ARCHIVE, "details", internalName)
    }

    fun add(
        providers: Providers,
        details: WorldMapAreaDetails,
        mapElements: List<WorldMapElement>,
    ) {
        val cacheProvider = providers.cacheProvider
        val (mapsquares, zones) = generateBlocks(details, providers.mapProvider, providers.objectProvider)
        val area = WorldMapArea(details.internalName, details, WorldMapAreaData(mapsquares, zones, mapElements))
        val (sprites, composite) = MapsquareGround.generateSprites(
            providers,
            area,
            config.blendBordersSeparately,
            details.backgroundColour,
            config.brightness
        )
        write(
            cacheProvider,
            composite,
            area,
            sprites,
            mapsquares,
            zones,
            details,
            mapElements,
            true
        )
    }

    fun update(
        id : Int,
        internalName: String,
        providers: Providers,
        detailsTransformer: (WorldMapAreaDetails) -> WorldMapAreaDetails,
        labelsTransformer: (List<WorldMapElement>) -> List<WorldMapElement>
    ) {
        val cacheProvider = providers.cacheProvider
        val area = WorldMapArea.decode(cacheProvider,id,internalName)
        val details = detailsTransformer(area.details)
        val labels = labelsTransformer(area.data.mapElements)
        val (mapsquares, zones) = generateBlocks(details, providers.mapProvider, providers.objectProvider)
        val newArea = WorldMapArea(area.internalName, details, WorldMapAreaData(mapsquares, zones, labels))
        val (sprites, composite) = MapsquareGround.generateSprites(
            providers,
            newArea,
            config.blendBordersSeparately,
            area.details.backgroundColour,
            config.brightness
        )
        write(
            cacheProvider,
            composite,
            newArea,
            sprites,
            mapsquares,
            zones,
            details,
            labels,
            false
        )
    }

    private fun write(
        cacheProvider: CacheProvider,
        composite: BufferedImage,
        area: WorldMapArea,
        sprites: Map<MapsquareId, BufferedImage>,
        mapsquares: List<WorldMapMapsquare>,
        zones: List<WorldMapZone>,
        details: WorldMapAreaDetails,
        labels: List<WorldMapElement>,
        add: Boolean,
    ) {
        val compositeOutput = ByteArrayOutputStream()
        ImageIO.write(composite, config.imageType, compositeOutput)

        // Write composite image to disk (debug/export)

        val outputDir = Paths.get("C:/Users/chris/Desktop/225 Cache/images")
        Files.createDirectories(outputDir)

        val safeName = area.details.internalName
            .filter { it.code >= 32 } // removes control chars
            .replace(Regex("""[<>:"/\\|?*]"""), "_")
            .take(120) // optional safety cap
        val outputFile = outputDir.resolve("$safeName.${config.imageType}").toFile()

        ImageIO.write(composite, config.imageType, outputFile)

        val compositeBuffer = Unpooled.wrappedBuffer(compositeOutput.toByteArray())
        cacheProvider.write(
            WORLD_MAP_DATA_ARCHIVE,
            "compositetexture",
            area.details.internalName,
            compositeBuffer
        )

        if (add) {
            cacheProvider.write(
                WORLD_MAP_DATA_ARCHIVE,
                area.details.internalName,
                "labels",
                Unpooled.wrappedBuffer(byteArrayOf(0))
            )
        }

        val remappedFiles = mutableMapOf<MapsquareId, Int>()

        for ((mapsquareId, image) in sprites) {
            val byteOutputStream = ByteArrayOutputStream()
            ImageIO.write(image, config.imageType, byteOutputStream)
            val buf = Unpooled.wrappedBuffer(byteOutputStream.toByteArray())
            val emptyGroupId = cacheProvider.allocateEmpty(WORLD_MAP_GROUND_ARCHIVE)
            cacheProvider.write(WORLD_MAP_GROUND_ARCHIVE, emptyGroupId, 0, buf)
            remappedFiles[mapsquareId] = emptyGroupId
        }

        val zoneGroupId = mutableMapOf<Int, Int>()

        val newMapsquares = mapsquares.map { msq ->
            val groupId = remappedFiles.getValue(
                MapsquareId(msq.data.mapsquareDestinationX, msq.data.mapsquareDestinationY)
            )
            val fileId = zoneGroupId.getOrElse(groupId) { 0 }
            zoneGroupId[groupId] = fileId + 1
            WorldMapMapsquare(
                msq.data.copy(groupId = groupId, fileId = fileId),
                msq.geography
            )
        }

        val newZones = zones.map { zone ->
            val groupId = remappedFiles.getValue(
                MapsquareId(zone.data.mapsquareDestinationX, zone.data.mapsquareDestinationY)
            )
            val fileId = zoneGroupId.getOrElse(groupId) { 0 }
            zoneGroupId[groupId] = fileId + 1
            WorldMapZone(
                zone.data.copy(groupId = groupId, fileId = fileId),
                zone.geography
            )
        }

        val areaData = WorldMapAreaData(newMapsquares, newZones, labels)

        val detailsBuffer = Unpooled.buffer(1000)
        details.encode(detailsBuffer)
//        cacheProvider.write(
//            WORLD_MAP_DATA_ARCHIVE,
//            "details",
//            details.internalName,
//            details.id,
//            detailsBuffer
//        )

        val dataBuffer = Unpooled.buffer(10_000)
        areaData.encode(dataBuffer)
        cacheProvider.write(
            WORLD_MAP_DATA_ARCHIVE,
            "compositemap",
            details.internalName,
            dataBuffer
        )

        for (mapsquare in newMapsquares) {
            val geographyBuffer = Unpooled.buffer(1000)
            mapsquare.geography.encode(geographyBuffer)
            cacheProvider.write(
                WORLD_MAP_GEOGRAPHY_ARCHIVE,
                mapsquare.data.groupId,
                mapsquare.data.fileId,
                geographyBuffer
            )
        }

        for (zone in newZones) {
            val geographyBuffer = Unpooled.buffer(1000)
            zone.geography.encode(geographyBuffer)
            cacheProvider.write(
                WORLD_MAP_GEOGRAPHY_ARCHIVE,
                zone.data.groupId,
                zone.data.fileId,
                geographyBuffer
            )
        }
    }

    private fun generateBlocks(
        details: WorldMapAreaDetails,
        mapProvider: MapProvider,
        objectProvider: ObjectProvider
    ): Pair<List<WorldMapMapsquare>, List<WorldMapZone>> {
        val mapsquares = mutableListOf<WorldMapMapsquare>()
        val zones = mutableListOf<WorldMapZone>()
        for (section in details.sections) {
            when (section) {
                is MapsquareSingleSection -> {
                    val builder = WorldMapSingleMapsquareBuilder(cache2,section)
                    mapsquares += builder.build(mapProvider, objectProvider)
                }
                is MapsquareMultiSection -> {
                    val builder = WorldMapMultiMapsquareBuilder(cache2,section)
                    mapsquares += builder.build(mapProvider, objectProvider)
                }
                is ZoneSingleSection -> {
                    val builder = WorldMapSingleZoneBuilder(cache2,section)
                    zones += builder.build(mapProvider, objectProvider)
                }
                is ZoneMultiSection -> {
                    val builder = WorldMapMultiZoneBuilder(cache2,section)
                    zones += builder.build(mapProvider, objectProvider)
                }
            }
        }


        return mapsquares to zones
    }
}
