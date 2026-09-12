package dev.openrune.cache.tools


import dev.openrune.OsrsCacheProvider
import dev.openrune.cache.MAPS
import dev.openrune.cache.WORLDMAPAREAS
import dev.openrune.cache.filestore.definition.FontDecoder
import dev.openrune.cache.filestore.definition.SpriteDecoder
import dev.openrune.cache.worldmap.worldmap.WorldMapAreaDetails
import dev.openrune.cache.worldmap.worldmap.WorldMapFormat
import dev.openrune.cache.worldmap.worldmap.exportBaseName
import dev.openrune.cache.worldmap.worldmap.hasValidNames
import dev.openrune.cache.worldmap.worldmap.sourceMapsquares
import dev.openrune.cache.worldmap.worldmap.randomExportName
import dev.openrune.cache.worldmap.worldmap.builder.test.WorldMap
import dev.openrune.cache.worldmap.worldmap.config.WorldMapConfig
import dev.openrune.cache.worldmap.worldmap.ground.MapsquareId
import dev.openrune.cache.worldmap.worldmap.providers.CacheProvider
import dev.openrune.cache.worldmap.worldmap.providers.Landscape
import dev.openrune.cache.worldmap.worldmap.providers.MapProvider
import dev.openrune.cache.worldmap.worldmap.providers.Mapsquare
import dev.openrune.cache.worldmap.worldmap.providers.ObjectProvider
import dev.openrune.cache.worldmap.worldmap.providers.OverlayProvider
import dev.openrune.cache.worldmap.worldmap.providers.Providers
import dev.openrune.cache.worldmap.worldmap.providers.Underlay
import dev.openrune.cache.worldmap.worldmap.providers.UnderlayProvider
import dev.openrune.cache.worldmap.worldmap.providers.WorldMapObject
import dev.openrune.cache.worldmap.rasterizer.provider.FontMetrics
import dev.openrune.cache.worldmap.rasterizer.provider.FontMetricsProvider
import dev.openrune.cache.worldmap.rasterizer.provider.SpriteProvider
import dev.openrune.cache.worldmap.worldmap.utils.Coordinate
import dev.openrune.cache.tools.progress.CacheProgress
import dev.openrune.cache.tools.progress.DefaultCacheProgress
import dev.openrune.cache.tools.worldmap.packing.FullMapDefinition
import dev.openrune.cache.worldmap.worldmap.providers.MapElement
import dev.openrune.cache.worldmap.worldmap.providers.MapElementConfigProvider
import dev.openrune.cache.worldmap.worldmap.providers.TextureProvider
import dev.openrune.cache.worldmap.rasterizer.provider.GraphicsDefaultsProvider
import dev.openrune.cache.util.logger
import dev.openrune.cache.worldmap.mapdecoder.MapLocDefinition
import dev.openrune.definition.type.MapElementType
import dev.openrune.definition.type.ObjectType
import dev.openrune.definition.type.OverlayType
import dev.openrune.definition.type.SpriteType
import dev.openrune.definition.type.TextureType
import dev.openrune.definition.type.UnderlayType
import dev.openrune.definition.util.toArray
import dev.openrune.filesystem.Cache
import readCacheRevision
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import java.io.IOException
import java.nio.file.Paths
import java.nio.file.Path
import javax.imageio.ImageIO

/**
 * @author Kris | 22/08/2022
 */
class WorldMapPacker(
    val cache: Cache,
    private val progress: CacheProgress = DefaultCacheProgress(),
) {
    val mapProvider = CachedMapProvider(cache)

    /** Builds every provider the renderer and packer need from the supplied cache. */
    fun buildProviders(): Providers = Providers(
        provideCache(cache),
        provideTextures(),
        provideSprites(),
        provideFonts(),
        provideObjects(),
        mapProvider,
        provideOverlays(),
        provideMapElements(),
        provideGraphicsDefaults(),
        provideUnderlays(),
    )

    private fun buildConfig() = WorldMapConfig().apply {
        cacheRevision = runCatching { readCacheRevision(cache) }.getOrDefault(WorldMapFormat.REVISION)
    }

    /**
     * Renders every world map area already present in the cache to a PNG in [outputDir].
     * Read-only: nothing is written back to the cache.
     */
    fun dumpImages(outputDir: Path, pixelsPerTile: Int = 4) {
        val providers = buildProviders()
        val config = buildConfig()
        val worldMap = WorldMap(config, cache)
        outputDir.toFile().mkdirs()
        val blocks = loadAreaBlocks(cache).sortedBy { it.details.id }
        progress.begin("Rendering World Map", blocks.size).use { tracker ->
            for (block in blocks) {
                val details = block.details
                tracker.message(details.displayName)
                if (!worldMap.exists(providers, details.id, details.internalName)) {
                    tracker.step()
                    continue
                }
                val outputFile = outputDir.resolve("${details.id}_${details.exportBaseName()}.png").toFile()
                try {
                    val image = worldMap.generateImageFromExistingData(
                        details.id,
                        details.internalName,
                        providers,
                        pixelsPerTile,
                    )
                    ImageIO.write(image, "png", outputFile)
                    logger.debug { "Wrote ${outputFile.name} (${image.width}x${image.height})" }
                } catch (e: Exception) {
                    logger.warn { "Failed to render area ${details.id} (${details.internalName}): $e" }
                }
                tracker.step()
            }
        }
    }

    /**
     * Packs [blocks] into the cache, updating areas that already exist and adding the rest.
     * When [imageOutputDir] is set, each updated area is also rendered to a PNG there.
     */
    fun pack(
        blocks: List<WorldMapAreaBlock>,
        imageOutputDir: Path? = null,
        dirtySourceMapsquares: Set<Int>? = null,
    ) {
        val startedAt = System.nanoTime()
        logger.info { "Loading world map providers (sprites, fonts, configs)..." }
        val providers = buildProviders()
        val config = buildConfig()
        val worldMap = WorldMap(config, cache)
        val providersMs = (System.nanoTime() - startedAt) / 1_000_000
        logger.info { "World map providers ready in ${providersMs}ms; packing ${blocks.size} area(s)." }
        var encodeNanos = 0L
        var imageNanos = 0L
        var updated = 0
        var added = 0

        val sorted = blocks.sortedBy { it.details.id }
        progress.begin("Packing World Map", sorted.size).use { tracker ->
            for ((index, block) in sorted.withIndex()) {
                tracker.message(block.details.displayName)
                if (worldMap.exists(providers, block.details.id, block.details.internalName)) {
                    updated++

                    val areaStartedAt = System.nanoTime()
                    worldMap.update(
                        block.details.id,
                        block.details.internalName,
                        providers,
                        detailsTransformer = { details ->
                            require(details.id == block.details.id)
                            require(details.internalName == block.details.internalName)
                            details.copy(
                                displayName = block.details.displayName,
                                origin = block.details.origin,
                                backgroundColour = block.details.backgroundColour,
                                mapBackgroundColour = block.details.mapBackgroundColour,
                                zoom = block.details.zoom,
                                // The block carries the full section list, so replace rather than append;
                                // appending would duplicate every section each time an area is repacked.
                                sections = block.details.sections,
                            )
                        },
                        labelsTransformer = { labels ->
                            if (block.mapElements.isEmpty()) labels else block.mapElements
                        },
                        dirtySourceMapsquares = dirtySourceMapsquares,
                    )
                    val areaMs = (System.nanoTime() - areaStartedAt) / 1_000_000
                    encodeNanos += System.nanoTime() - areaStartedAt
                    logger.debug {
                        "[${index + 1}/${sorted.size}] Updated ${block.details.displayName} " +
                            "(id ${block.details.id}) in ${areaMs}ms"
                    }

                    if (imageOutputDir != null) {
                        val imageStartedAt = System.nanoTime()
                        val outputFile = imageOutputDir
                            .resolve("${block.details.id}_${block.details.exportBaseName()}.png")
                            .toFile()
                        outputFile.parentFile?.mkdirs()
                        ImageIO.write(
                            worldMap.generateImageFromExistingData(block.details.id, block.details.internalName, providers, 4),
                            "png",
                            outputFile,
                        )
                        imageNanos += System.nanoTime() - imageStartedAt
                        logger.debug { "Wrote ${outputFile.name}" }
                    }
                } else {
                    added++
                    val areaStartedAt = System.nanoTime()
                    worldMap.add(providers, block.details, block.mapElements)
                    encodeNanos += System.nanoTime() - areaStartedAt
                    logger.debug {
                        "[${index + 1}/${sorted.size}] Added ${block.details.displayName} " +
                            "(id ${block.details.id}) in ${(System.nanoTime() - areaStartedAt) / 1_000_000}ms"
                    }
                }
                tracker.step()
            }
        }

        val flushStartedAt = System.nanoTime()
        logger.info { "Flushing world map changes to cache..." }
        cache.update()
        val flushMs = (System.nanoTime() - flushStartedAt) / 1_000_000
        val encodeMs = encodeNanos / 1_000_000
        val totalMs = (System.nanoTime() - startedAt) / 1_000_000
        logger.info {
            "World map pack: $updated updated, $added added in ${totalMs}ms " +
                "(providers ${providersMs}ms, encode ${encodeMs}ms, cache flush ${flushMs}ms" +
                (if (imageOutputDir != null) ", png ${imageNanos / 1_000_000}ms" else "") + ")"
        }
    }

    /** Repacks every world map area already present in the cache. */
    fun repack(imageOutputDir: Path? = null) = pack(loadAreaBlocks(cache), imageOutputDir)

    /**
     * Repacks only the world map areas that draw from one of [changedMapsquares], given as
     * `(x shl 8) or y`. Areas that source none of those squares are left untouched.
     * Returns the number of areas repacked.
     */
    fun repackChanged(changedMapsquares: Set<Int>, imageOutputDir: Path? = null): Int {
        if (changedMapsquares.isEmpty()) {
            logger.info { "No mapsquares changed; world map left as-is." }
            return 0
        }
        val affected = loadAreaBlocks(cache).filter { block ->
            block.details.sections.any { section ->
                section.sourceMapsquares().any { it in changedMapsquares }
            }
        }
        if (affected.isEmpty()) {
            logger.info { "${changedMapsquares.size} mapsquare(s) changed, none used by a world map area." }
            return 0
        }
        logger.info {
            "${changedMapsquares.size} mapsquare(s) changed; repacking ${affected.size} world map area(s): " +
                affected.joinToString { it.details.internalName }
        }
        pack(affected, imageOutputDir, dirtySourceMapsquares = changedMapsquares)
        return affected.size
    }

    private fun provideMapElements(): MapElementConfigProvider {
        val configs = mutableMapOf<Int, MapElementType>()
        OsrsCacheProvider.AreaDecoder().load(cache,configs)

        return object : MapElementConfigProvider {
            override fun getMapElement(id: Int): MapElement {
                val config = configs[id] ?: throw IllegalArgumentException()
                return object : MapElement {
                    override val text: String? get() = if (config.name == "null") null else config.name
                    override val textSize: Int get() = config.textSize
                    override val textColour: Int get() = config.fontColor
                    override val graphic: Int get() = config.sprite1
                    override val horizontalAlignment: Int get() = config.horizontalAlignment
                    override val verticalAlignment: Int get() = config.verticalAlignment
                }
            }
        }
    }

    private fun provideOverlays(): OverlayProvider {
        val configs = mutableMapOf<Int, OverlayType>()
        OsrsCacheProvider.OverlayDecoder().load(cache,configs)
        return object : OverlayProvider {
            fun getConfig(id : Int) = configs[id]?: error("Overlay config not found for id $id")
            override fun exists(id: Int): Boolean {
                return configs[id] != null
            }

            override fun getTextureId(id: Int): Int {
                return getConfig(id).texture
            }

            override fun getMinimapColour(id: Int): Int {
                return getConfig(id).secondaryRgb
            }

            override fun getTileColour(id: Int): Int {
                return getConfig(id).primaryRgb
            }

            override fun getHue(id: Int): Int {
                return getConfig(id).hue
            }

            override fun getSaturation(id: Int): Int {
                return getConfig(id).saturation
            }

            override fun getLightness(id: Int): Int {
                return getConfig(id).lightness
            }
        }
    }

    private fun provideTextures(): TextureProvider {
        val textures = mutableMapOf<Int, TextureType>()
        OsrsCacheProvider.TextureDecoder(238).load(cache,textures)

        return object : TextureProvider {
            override fun getHsl(textureId: Int): Int {
                return textures[textureId]?.averageRgb ?: return -1
            }
        }
    }

    private fun provideFonts(): FontMetricsProvider {
        val fonts = FontDecoder(cache).loadAllFonts()

        return object : FontMetricsProvider {
            override val verdana11FontId: Int = 1442
            override val verdana13FontId: Int = 1445
            override val verdana15FontId: Int = 1447
            override fun getFont(id: Int): FontMetrics? {
                val font = fonts[id] ?: return null
                return object : FontMetrics {
                    override val advances: IntArray = font.glyphAdvances
                    override val kerning: ByteArray? = font.kerning.takeIf { it.isNotEmpty() }
                    override val ascent: Int = font.ascent
                }
            }
        }
    }

    private fun provideSprites(): SpriteProvider {
        val sprites = mutableMapOf<Int, SpriteType>()
        SpriteDecoder().load(cache, sprites)
        return CachedSpriteProvider(sprites)
    }

    class CachedSpriteProvider(private val archive: MutableMap<Int, SpriteType>) : SpriteProvider {
        override val verdana11PtId: Int = 1442
        override val verdana13ptId: Int = 1445
        override val verdana15ptId: Int = 1447
        override fun getSprites(): Map<Int, SpriteType> {
           return archive
        }
    }

    private fun provideUnderlays(): UnderlayProvider {
        val underlays = mutableMapOf<Int, UnderlayType>()
        OsrsCacheProvider.UnderlayDecoder().load(cache,underlays)

        return object : UnderlayProvider {
            override fun getUnderlay(id: Int): Underlay? {
                val config = underlays[id] ?: return null
                return object : Underlay {
                    override val hue: Int get() = config.hue
                    override val hueMultiplier: Int get() = config.hueMultiplier
                    override val saturation: Int get() = config.saturation
                    override val lightness: Int get() = config.lightness
                }
            }
        }
    }

    private fun provideGraphicsDefaults(): GraphicsDefaultsProvider {
        return object : GraphicsDefaultsProvider {
            override fun getMapScenesGroup(): Int {
                return 317
            }

            override fun getModIconsGroup(): Int {
                return 423
            }
        }
    }

    private fun provideObjects(): ObjectProvider {

        val objects = mutableMapOf<Int, ObjectType>()
        OsrsCacheProvider.ObjectDecoder(238).load(cache,objects)

        // postDecode returns a copy now that definitions are immutable.
        objects.entries.forEach { entry ->
            entry.setValue(entry.value.postDecode())
        }

        return object : ObjectProvider {
            override fun getMapSceneId(id: Int): Int {
                val obj = objects[id]
                return obj?.mapSceneID?: -1
            }

            override fun getMapIconId(id: Int): Int {
                val obj = objects[id]
                return obj?.mapAreaId?: -1
            }

            override fun getBoundaryType(id: Int): Int {
                val obj = objects[id]
                return obj?.interactive?: -1
            }
        }
    }

    class CachedMapProvider(gameCache : Cache) : MapProvider {
        data class Loc(override val id: Int, override val shape: Int, override val rotation: Int, override val coordinate: Coordinate) : WorldMapObject
        private val cache = mutableMapOf<MapsquareId, Mapsquare?>()

        fun storeBuf(mapsquareId: MapsquareId, mapBuffer: ByteBuf, locBuffer: ByteBuf?): Mapsquare {
            val fullMap = FullMapDefinition.decode(mapBuffer, mapsquareId.x, mapsquareId.y)
            val mapObjects = locBuffer?.let {
                try {
                    MapLocDefinition.decodeBaseData(it)
                } catch (e: Exception) {
                    logger.warn { "Unreadable loc data for mapsquare ${mapsquareId.x},${mapsquareId.y}: $e" }
                    emptyList()
                }
            } ?: emptyList()

            mapBuffer.release()
            locBuffer?.release()
            val worldMapObjects = mapObjects.map { loc ->
                Loc(loc.id, loc.type, loc.orientation, loc.coordinate)
            }
            // Same thing here..
            val land = object : Landscape {
                override fun getUnderlayId(level: Int, x: Int, y: Int): Int {
                    return fullMap.underlayIds[level][x][y] - 1
                }

                override fun getOverlayId(level: Int, x: Int, y: Int): Int {
                    return fullMap.overlayIds[level][x][y] - 1
                }

                override fun getOverlayShape(level: Int, x: Int, y: Int): Int {
                    return fullMap.overlayPaths[level][x][y].toInt()
                }

                override fun getOverlayRotation(level: Int, x: Int, y: Int): Int {
                    return fullMap.overlayRotations[level][x][y].toInt()
                }

                override fun getFlags(level: Int, x: Int, y: Int): Int {
                    return fullMap.tileSettings[level][x][y].toInt()
                }
            }
            val cacheMap = Mapsquare(land, worldMapObjects)
            cache[mapsquareId] = cacheMap
            return cacheMap
        }

        override fun getMap(cache: Cache, mapsquareX: Int, mapsquareY: Int): Mapsquare? {
            val mapsquareId = MapsquareId(mapsquareX, mapsquareY)
            val cached = this@CachedMapProvider.cache[mapsquareId]
            if (cached != null) return cached
            val (mapBuffer, locBuffer) = readMapBuffers(cache,mapsquareX, mapsquareY) ?: return null
            return storeBuf(mapsquareId, mapBuffer, locBuffer)
        }

        private fun readMapBuffers(
            cacheProvider: Cache,
            mapsquareX: Int,
            mapsquareY: Int
        ): Pair<ByteBuf, ByteBuf?>? {

            val groupId = (mapsquareX shl 8) or (mapsquareY and 0xFF)

            val mapData = try {
                cacheProvider.data(MAPS, groupId, 0) ?: return null
            } catch (e: IOException) {
                return null
            }

            val locData = try {
                cacheProvider.data(MAPS, groupId, 1)
            } catch (e: IOException) {
                null
            }

            val mapFile = Unpooled.wrappedBuffer(mapData)
            val locFile = locData?.let { Unpooled.wrappedBuffer(it) }

            return mapFile to locFile
        }
    }

    private fun loadAreaBlocks(cache: Cache): List<WorldMapAreaBlock> {
        val blocks = mutableListOf<WorldMapAreaBlock>()
        val detailsGroup = WorldMapFormat.detailsGroupId()
        for (fileId in cache.files(WORLDMAPAREAS, detailsGroup)) {
            val data = cache.data(WORLDMAPAREAS, detailsGroup, fileId) ?: continue
            try {
                val details = WorldMapAreaDetails.decode(fileId, Unpooled.wrappedBuffer(data))
                if (!details.hasValidNames()) continue
                blocks += WorldMapAreaBlock(details, emptyList())
            } catch (_: Exception) {
            }
        }
        return blocks
    }

    private fun provideCache(cache2: Cache): CacheProvider {
        return object : CacheProvider() {
            override fun read(archive: Int, group: Int, file: Int): ByteBuf {
                return Unpooled.wrappedBuffer(cache2.data(archive, group, file))
            }

            override fun read(archive: Int, group: String, file: String): ByteBuf {
                error("Not implemented: 1")
                return Unpooled.wrappedBuffer(byteArrayOf())
            }

            override fun exists(archive: Int, group: Int, file: Int): Boolean {
                return cache2.data(archive, group, file) != null
            }

            override fun exists(archive: Int, group: String, file: String): Boolean {
                error("Not implemented: 2")
                return false
            }

            override fun write(archive: Int, group: Int, file: Int, buf: ByteBuf) {
                cache2.write(archive, group, file, buf.toArray())
            }

            override fun write(archive: Int, group: String, file: String, buf: ByteBuf) {
                //require(buf.isReadable)
                //cache2.write(archive, group, file, buf)
                //todo
            }

            override fun write(archive: Int, group: String, file: String, fileId: Int, buf: ByteBuf) {
                error("Not implemented: 4")
            }

            override fun list(archive: Int): List<Int> {
                return cache.archives(archive).asSequence().map { it }.toList()
            }

            override fun list(archive: Int, group: Int): List<Int> {
                return cache.files(archive, group).asSequence().map { it }.toList()
            }

            override fun list(archive: Int, group: String): List<Int> {
                error("Not implemented: 5")
                return emptyList()
            }
        }
    }
}

