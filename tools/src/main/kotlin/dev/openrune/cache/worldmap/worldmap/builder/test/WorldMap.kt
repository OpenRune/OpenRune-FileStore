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
import dev.openrune.cache.util.logger
import dev.openrune.filesystem.Cache
import io.netty.buffer.Unpooled
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.nio.file.Files
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
        labelsTransformer: (List<WorldMapElement>) -> List<WorldMapElement>,
        regenerateGeography: Boolean = true,
        dirtySourceMapsquares: Set<Int>? = null,
    ) {
        val cacheProvider = providers.cacheProvider
        val t0 = System.nanoTime()
        // An incremental update reads the placements first with no geography at all, works out the few
        // squares it needs, then decodes only those. A full update decodes everything up front.
        var plan: IncrementalPlan? = null
        val area = if (dirtySourceMapsquares == null || !regenerateGeography) {
            WorldMapArea.decode(cacheProvider, id, internalName, config.cacheRevision)
        } else {
            val placements = WorldMapArea.decode(
                cacheProvider, id, internalName, config.cacheRevision, geographyFilter = { _, _ -> false },
            )
            plan = planIncremental(placements, dirtySourceMapsquares)
            if (plan == null) {
                logger.info { "$internalName: no changed mapsquares in this area, skipping." }
                return
            }
            val paint = plan.paint
            WorldMapArea.decode(
                cacheProvider, id, internalName, config.cacheRevision,
                geographyFilter = { x, y -> MapsquareId(x, y) in paint },
            )
        }
        val decodeMs = (System.nanoTime() - t0) / 1_000_000
        val details = detailsTransformer(area.details)
        val labels = labelsTransformer(area.data.mapElements)
        val t1 = System.nanoTime()
        // Rebuilding from the landscape is what picks up map edits; pass false to round-trip the
        // geography already in the cache instead.
        val mapsquares: List<WorldMapMapsquare>
        val zones: List<WorldMapZone>
        // Squares whose ground image has to be re-blended. null means "all of them".
        var groundRegenerate: Set<MapsquareId>? = null
        var groundPaint: Set<MapsquareId>? = null
        val currentPlan = plan
        if (currentPlan != null) {
            mapsquares = area.data.mapsquares.map { square ->
                if (MapsquareId(square.data.mapsquareDestinationX, square.data.mapsquareDestinationY) in currentPlan.geography) {
                    regenerateMapsquare(square, providers) ?: square
                } else {
                    square
                }
            }
            zones = area.data.zones.map { zone ->
                if (MapsquareId(zone.data.mapsquareDestinationX, zone.data.mapsquareDestinationY) in currentPlan.geography) {
                    regenerateZone(zone, providers) ?: zone
                } else {
                    zone
                }
            }
            groundRegenerate = currentPlan.ground
            groundPaint = currentPlan.paint
            logger.debug {
                "  squares rewritten: " + currentPlan.ground.sortedBy { it.packed }.joinToString { "${it.x},${it.y}" }
            }
            logger.info {
                "$internalName: incremental update, ${currentPlan.geography.size} square(s) regenerated, " +
                    "${currentPlan.ground.size} re-blended (${currentPlan.paint.size} painted) of " +
                    "${area.data.mapsquares.size + area.data.zones.size}"
            }
        } else if (regenerateGeography) {
            val generated = generateBlocks(details, providers.mapProvider, providers.objectProvider)
            mapsquares = generated.first
            zones = generated.second
        } else {
            mapsquares = area.data.mapsquares
            zones = area.data.zones
        }
        val blocksMs = (System.nanoTime() - t1) / 1_000_000
        val newArea = WorldMapArea(area.internalName, details, WorldMapAreaData(mapsquares, zones, labels))
        val t2 = System.nanoTime()
        val (sprites, composite) = MapsquareGround.generateSprites(
            providers,
            newArea,
            config.blendBordersSeparately,
            area.details.backgroundColour,
            config.brightness,
            paint = groundPaint,
            compositeRegenerate = groundRegenerate,
            cachedComposite = if (groundRegenerate == null) null else readCachedComposite(cacheProvider, id),
        )
        val spritesMs = (System.nanoTime() - t2) / 1_000_000
        val t3 = System.nanoTime()
        write(
            cacheProvider,
            composite,
            newArea,
            sprites,
            mapsquares,
            zones,
            details,
            labels,
            false,
            groundRegenerate,
        )
        logger.debug {
            "  $internalName: decode ${decodeMs}ms, blocks ${blocksMs}ms, sprites ${spritesMs}ms, " +
                "write ${(System.nanoTime() - t3) / 1_000_000}ms"
        }
    }

    private class IncrementalPlan(
        /** Squares whose geography must be rebuilt from the landscape. */
        val geography: Set<MapsquareId>,
        /** Those squares plus a one-square margin: the grounds whose pixels actually change. */
        val ground: Set<MapsquareId>,
        /** [ground] plus another square of margin: everything that must be painted for [ground] to be complete. */
        val paint: Set<MapsquareId>,
    )

    /**
     * Works out the smallest set of squares that has to be rebuilt for [dirtySourceMapsquares].
     * Returns null when this area reads none of the changed squares.
     *
     * Ground blending samples a five tile radius, so a changed square can only bleed into its eight
     * immediate neighbours; those get re-blended but keep their existing geography.
     */
    private fun planIncremental(area: WorldMapArea, dirtySourceMapsquares: Set<Int>): IncrementalPlan? {
        val geography = HashSet<MapsquareId>()
        for (square in area.data.mapsquares) {
            val source = (square.data.mapsquareSourceX shl 8) or (square.data.mapsquareSourceY and 0xFF)
            if (source in dirtySourceMapsquares) {
                geography += MapsquareId(square.data.mapsquareDestinationX, square.data.mapsquareDestinationY)
            }
        }
        for (zone in area.data.zones) {
            val source = (zone.data.mapsquareSourceX shl 8) or (zone.data.mapsquareSourceY and 0xFF)
            if (source in dirtySourceMapsquares) {
                geography += MapsquareId(zone.data.mapsquareDestinationX, zone.data.mapsquareDestinationY)
            }
        }
        if (geography.isEmpty()) return null
        val ground = expand(geography)
        // Painting square N also writes into N's neighbours, so N's ground is only complete once every
        // neighbour has been painted too. That needs one more ring than the grounds we intend to write.
        return IncrementalPlan(geography, ground, expand(ground))
    }

    private fun expand(squares: Set<MapsquareId>): Set<MapsquareId> {
        val expanded = HashSet<MapsquareId>(squares)
        for (id in squares) {
            for (dx in -1..1) {
                for (dy in -1..1) {
                    expanded += MapsquareId(id.x + dx, id.y + dy)
                }
            }
        }
        return expanded
    }

    /** Rebuilds one mapsquare's geography from the landscape, reusing the placement already recorded. */
    private fun regenerateMapsquare(square: WorldMapMapsquare, providers: Providers): WorldMapMapsquare? {
        val data = square.data
        val section = MapsquareSingleSection(
            data.level,
            data.levelsCount,
            data.mapsquareSourceX,
            data.mapsquareSourceY,
            data.mapsquareDestinationX,
            data.mapsquareDestinationY,
        )
        return WorldMapSingleMapsquareBuilder(cache, section)
            .build(providers.mapProvider, providers.objectProvider)
            .firstOrNull()
    }

    /** Rebuilds one zone's geography from the landscape, reusing the placement already recorded. */
    private fun regenerateZone(zone: WorldMapZone, providers: Providers): WorldMapZone? {
        val data = zone.data
        val section = ZoneSingleSection(
            data.level,
            data.levelsCount,
            data.mapsquareSourceX,
            data.zoneSourceX,
            data.mapsquareSourceY,
            data.zoneSourceY,
            data.mapsquareDestinationX,
            data.zoneDestinationX,
            data.mapsquareDestinationY,
            data.zoneDestinationY,
        )
        return WorldMapSingleZoneBuilder(cache, section)
            .build(providers.mapProvider, providers.objectProvider)
            .firstOrNull()
    }

    /**
     * Reads the composite texture already packed for this area. Squares that are not being rebuilt are
     * copied straight out of it instead of being redrawn.
     */
    private fun readCachedComposite(cacheProvider: CacheProvider, areaId: Int): BufferedImage? {
        val group = WorldMapFormat.compositetextureGroupId()
        if (!cacheProvider.exists(WORLD_MAP_DATA_ARCHIVE, group, areaId)) return null
        val buffer = cacheProvider.read(WORLD_MAP_DATA_ARCHIVE, group, areaId)
        return try {
            buffer.toImage()
        } catch (e: Exception) {
            null
        } finally {
            buffer.release()
        }
    }

    /** Encodes images to [WorldMapConfig.imageType] bytes in parallel, preserving input order. */
    private fun encodeImages(images: List<Pair<Int, BufferedImage>>): List<Pair<Int, ByteArray>> {
        if (images.size < 2) {
            return images.map { (key, image) ->
                key to ByteArrayOutputStream().also { ImageIO.write(image, config.imageType, it) }.toByteArray()
            }
        }
        return images.parallelStream()
            .map { (key, image) ->
                key to ByteArrayOutputStream().also { ImageIO.write(image, config.imageType, it) }.toByteArray()
            }
            .collect(java.util.stream.Collectors.toList())
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
        groundRegenerate: Set<MapsquareId>? = null,
    ) {
        val compositeOutput = ByteArrayOutputStream()
        ImageIO.write(composite, config.imageType, compositeOutput)

        // Write composite image to disk (debug/export), only when a directory is configured.
        val dumpDirectory = config.compositeDumpDirectory
        if (dumpDirectory != null) {
            Files.createDirectories(dumpDirectory)
            val safeName = area.details.internalName
                .filter { it.code >= 32 } // removes control chars
                .replace(Regex("""[<>:"/\\|?*]"""), "_")
                .take(120) // optional safety cap
            ImageIO.write(composite, config.imageType, dumpDirectory.resolve("$safeName.${config.imageType}").toFile())
        }

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
            // One ground image per square. Encoding dominates this loop and each image is independent,
            // so encode in parallel and keep the cache writes on this thread.
            val writtenGround = mutableSetOf<Int>()
            val toEncode = ArrayList<Pair<Int, BufferedImage>>(sprites.size)
            for ((mapsquareId, image) in sprites) {
                // An untouched square still holds the bytes already in the cache; no need to rewrite it.
                if (groundRegenerate != null && mapsquareId !in groundRegenerate) continue
                val groupKey = WorldMapFormat.regionGroupKey(mapsquareId.x, mapsquareId.y)
                if (!writtenGround.add(groupKey)) continue
                toEncode += groupKey to image
            }
            val encStart = System.nanoTime()
            val encoded = encodeImages(toEncode)
            val encMs = (System.nanoTime() - encStart) / 1_000_000
            val wStart = System.nanoTime()
            for ((groupKey, bytes) in encoded) {
                cacheProvider.write(WORLD_MAP_GROUND_ARCHIVE, groupKey, areaId, Unpooled.wrappedBuffer(bytes))
            }
            logger.info {
                "    ground: ${toEncode.size} squares, encode ${encMs}ms, cacheWrite ${(System.nanoTime() - wStart) / 1_000_000}ms"
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

        writeWorldMapGeography(
            cacheProvider,
            areaId,
            newMapsquares,
            newZones,
            legacy,
            restrictToGroups = groundRegenerate?.mapTo(HashSet()) {
                WorldMapFormat.regionGroupKey(it.x, it.y)
            },
        )
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
