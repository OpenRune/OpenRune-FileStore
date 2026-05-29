@file:Suppress("unused")

package dev.openrune.cache.worldmap.worldmap.builder.test

import dev.openrune.cache.worldmap.worldmap.WorldMapAreaDetails
import dev.openrune.cache.worldmap.worldmap.*
import dev.openrune.cache.worldmap.worldmap.builder.blocks.WorldMapMultiMapsquareBuilder
import dev.openrune.cache.worldmap.worldmap.builder.blocks.WorldMapMultiZoneBuilder
import dev.openrune.cache.worldmap.worldmap.builder.blocks.WorldMapSingleMapsquareBuilder
import dev.openrune.cache.worldmap.worldmap.builder.blocks.WorldMapSingleZoneBuilder
import dev.openrune.cache.worldmap.worldmap.config.WorldMapConfig
import dev.openrune.cache.worldmap.worldmap.ground.MapsquareGround
import dev.openrune.cache.worldmap.worldmap.ground.MapsquareId
import dev.openrune.cache.worldmap.worldmap.providers.*
import dev.openrune.filesystem.Cache
import io.netty.buffer.Unpooled
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.nio.file.Paths
import javax.imageio.ImageIO

/**
 * @author Kris | 18/08/2022
 */
class WorldMap(private val config: WorldMapConfig, val cache: Cache) {

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
            generateUnderlays = true,
            revision = config.cacheRevision,
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
            WorldMapArea.decode(providers.cacheProvider, id, rscmName, config.cacheRevision),
            bordersSeparate,
            pixelsPerTile,
            config.brightness,
            generateUnderlays = false,
            revision = config.cacheRevision,
        )
    }

    fun exists(providers: Providers, areaId: Int, internalName: String): Boolean {
        return if (WorldMapFormat.isLegacy(config.cacheRevision)) {
            providers.cacheProvider.exists(WORLD_MAP_DATA_ARCHIVE, "details", internalName)
        } else {
            providers.cacheProvider.exists(WORLD_MAP_DATA_ARCHIVE, WorldMapFormat.detailsGroupId(), areaId)
        }
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
        val area = WorldMapArea.decode(cacheProvider, id, internalName, config.cacheRevision)
        val details = detailsTransformer(area.details)
        val labels = labelsTransformer(area.data.mapElements)
        // Keep geography from cache (round-trip). Regenerating from landscape here breaks repacked 238+ maps.
        val mapsquares = area.data.mapsquares
        val zones = area.data.zones
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

        val outputDir = Paths.get("C:\\Users\\chris\\Desktop\\Images\\img")
        Files.createDirectories(outputDir)

        val safeName = area.details.internalName
            .filter { it.code >= 32 } // removes control chars
            .replace(Regex("""[<>:"/\\|?*]"""), "_")
            .take(120) // optional safety cap
        val outputFile = outputDir.resolve("$safeName.${config.imageType}").toFile()

        ImageIO.write(composite, config.imageType, outputFile)

        val compositeBuffer = Unpooled.wrappedBuffer(compositeOutput.toByteArray())
        val legacy = WorldMapFormat.isLegacy(config.cacheRevision)
        val areaId = details.id

        if (legacy) {
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
        } else {
            cacheProvider.write(
                WORLD_MAP_DATA_ARCHIVE,
                WorldMapFormat.compositetextureGroupId(),
                areaId,
                compositeBuffer
            )
        }

        val newMapsquares: List<WorldMapMapsquare>
        val newZones: List<WorldMapZone>

        if (legacy) {
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
            newMapsquares = mapsquares.map { msq ->
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
            newZones = zones.map { zone ->
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
        } else {
            val writtenGround = mutableSetOf<Int>()
            for ((mapsquareId, image) in sprites) {
                val groupKey = WorldMapFormat.regionGroupKey(mapsquareId.x, mapsquareId.y)
                if (!writtenGround.add(groupKey)) continue
                val byteOutputStream = ByteArrayOutputStream()
                ImageIO.write(image, config.imageType, byteOutputStream)
                val buf = Unpooled.wrappedBuffer(byteOutputStream.toByteArray())
                cacheProvider.write(WORLD_MAP_GROUND_ARCHIVE, groupKey, areaId, buf)
            }
            newMapsquares = mapsquares.map { msq ->
                WorldMapMapsquare(msq.data.copy(groupId = -1, fileId = -1), msq.geography)
            }
            newZones = zones.map { zone ->
                WorldMapZone(zone.data.copy(groupId = -1, fileId = -1), zone.geography)
            }
        }

        val areaData = WorldMapAreaData(newMapsquares, newZones, labels)

        val detailsBuffer = Unpooled.buffer(1000)
        details.encode(detailsBuffer)
        if (legacy) {
            cacheProvider.write(
                WORLD_MAP_DATA_ARCHIVE,
                "details",
                details.internalName,
                detailsBuffer,
            )
        } else {
            cacheProvider.write(
                WORLD_MAP_DATA_ARCHIVE,
                WorldMapFormat.detailsGroupId(),
                areaId,
                detailsBuffer,
            )
        }

        val dataBuffer = Unpooled.buffer(10_000)
        areaData.encode(dataBuffer, config.cacheRevision)
        if (legacy) {
            cacheProvider.write(
                WORLD_MAP_DATA_ARCHIVE,
                "compositemap",
                details.internalName,
                dataBuffer
            )
        } else {
            cacheProvider.write(
                WORLD_MAP_DATA_ARCHIVE,
                WorldMapFormat.compositemapGroupId(),
                areaId,
                dataBuffer
            )
        }

        writeWorldMapGeography(cacheProvider, areaId, newMapsquares, newZones, legacy)
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
                    val builder = WorldMapSingleMapsquareBuilder(cache,section)
                    mapsquares += builder.build(mapProvider, objectProvider)
                }
                is MapsquareMultiSection -> {
                    val builder = WorldMapMultiMapsquareBuilder(cache,section)
                    mapsquares += builder.build(mapProvider, objectProvider)
                }
                is ZoneSingleSection -> {
                    val builder = WorldMapSingleZoneBuilder(cache,section)
                    zones += builder.build(mapProvider, objectProvider)
                }
                is ZoneMultiSection -> {
                    val builder = WorldMapMultiZoneBuilder(cache,section)
                    zones += builder.build(mapProvider, objectProvider)
                }
            }
        }


        return mapsquares to zones
    }
}
