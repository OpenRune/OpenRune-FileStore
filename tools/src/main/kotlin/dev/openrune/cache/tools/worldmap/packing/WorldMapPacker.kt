package dev.openrune.cache.tools.worldmap.packing


import dev.openrune.OsrsCacheProvider
import dev.openrune.cache.MAPS
import dev.openrune.cache.WORLDMAPAREAS
import dev.openrune.cache.filestore.definition.FontDecoder
import dev.openrune.cache.filestore.definition.SpriteDecoder
import dev.openrune.cache.tools.worldmap.WorldMapAreaDetails
import dev.openrune.cache.tools.worldmap.builder.test.WorldMap
import dev.openrune.cache.tools.worldmap.config.WorldMapConfig
import dev.openrune.cache.tools.worldmap.ground.MapsquareId
import dev.openrune.cache.tools.worldmap.providers.CacheProvider
import dev.openrune.cache.tools.worldmap.providers.Landscape
import dev.openrune.cache.tools.worldmap.providers.MapProvider
import dev.openrune.cache.tools.worldmap.providers.Mapsquare
import dev.openrune.cache.tools.worldmap.providers.ObjectProvider
import dev.openrune.cache.tools.worldmap.providers.OverlayProvider
import dev.openrune.cache.tools.worldmap.providers.Providers
import dev.openrune.cache.tools.worldmap.providers.Underlay
import dev.openrune.cache.tools.worldmap.providers.UnderlayProvider
import dev.openrune.cache.tools.worldmap.providers.WorldMapObject
import dev.openrune.cache.tools.worldmap.rasterizer.provider.FontMetrics
import dev.openrune.cache.tools.worldmap.rasterizer.provider.FontMetricsProvider
import dev.openrune.cache.tools.worldmap.rasterizer.provider.SpriteFrame
import dev.openrune.cache.tools.worldmap.rasterizer.provider.SpriteProvider
import dev.openrune.cache.tools.worldmap.rasterizer.provider.SpriteSheet
import dev.openrune.cache.tools.worldmap.utils.Coordinate
import dev.openrune.cache.tools.worldmap.packing.worldmap.TomlWorldMap
import dev.openrune.cache.tools.worldmap.packing.worldmap.WorldMapAreaBlock
import dev.openrune.cache.tools.worldmap.providers.MapElement
import dev.openrune.cache.tools.worldmap.providers.MapElementConfigProvider
import dev.openrune.cache.tools.worldmap.providers.TextureProvider
import dev.openrune.cache.tools.worldmap.rasterizer.provider.GraphicsDefaultsProvider
import dev.openrune.cache.util.logger
import dev.openrune.definition.constants.ConstantProvider
import dev.openrune.definition.type.MapElementType
import dev.openrune.definition.type.ObjectType
import dev.openrune.definition.type.OverlayType
import dev.openrune.definition.type.SpriteType
import dev.openrune.definition.type.TextureType
import dev.openrune.definition.type.UnderlayType
import dev.openrune.filesystem.Cache
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import kotlinx.serialization.decodeFromString
import net.peanuuutz.tomlkt.Toml
import java.io.IOException
import java.nio.file.Paths
import org.apache.commons.io.FileUtils
import org.apache.commons.io.filefilter.TrueFileFilter
import java.nio.file.Path
import javax.imageio.ImageIO
import kotlin.collections.get
import kotlin.collections.iterator

/**
 * @author Kris | 22/08/2022
 */
class WorldMapPacker(val cache: Cache) {
    val mapProvider = CachedMapProvider(cache)

    fun pack(cachePath : Path) {
        val packSurface = true
        val providers = Providers(
            provideCache(cachePath),
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
        val config = WorldMapConfig()
        val worldMap = WorldMap(config,cache, org.openrs2.cache.Cache.open(cachePath))
        val blocks = mutableListOf<WorldMapAreaBlock>()

        cache.archives(WORLDMAPAREAS).forEach { archive ->
            cache.files(WORLDMAPAREAS, archive).forEach { id ->

                val data = cache.data(WORLDMAPAREAS, archive, id)
                    ?: return@forEach

                try {
                    val block = WorldMapAreaBlock(
                        details = WorldMapAreaDetails.decode(
                            id,
                            Unpooled.wrappedBuffer(data)
                        ),
                        mapElements = emptyList()
                    )

                    blocks += block
                }catch (e : Exception) {

                }

            }
        }

        for (block in blocks.sortedBy { it.details.id }) {
            if (block.details.isMain) {
                if (packSurface != true) continue
            }
            if (worldMap.exists(providers, block.details.internalName)) {
                println("HERE2")
                logger.info { "Updating ${block.details.displayName} map area." }

                val name = block.details.internalName.ifEmpty { block.details.displayName }.ifEmpty { "unnamed_${block.details.id}" }

                ImageIO.write(worldMap.generateImageFromExistingData(block.details.id,block.details.internalName,providers, 4), "png", Paths.get("C:\\Users\\chris\\Desktop\\225 Cache\\images\\${name}.png").toFile())
//                worldMap.update(
//                    block.details.id,
//                    block.details.internalName,
//                    providers,
//                    detailsTransformer = { details ->
//                        require(details.id == block.details.id)
//                        require(details.internalName == block.details.internalName)
//                        details.copy(
//                            displayName = block.details.displayName,
//                            origin = block.details.origin,
//                            backgroundColour = block.details.backgroundColour,
//                            zoom = block.details.zoom,
//                            sections = details.sections + block.details.sections
//                        )
//                    },
//                    labelsTransformer = { labels ->
//                        labels + block.mapElements
//                    })


            } else {
                worldMap.add(providers, block.details, block.mapElements)
            }
        }
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
                    override val kerning: ByteArray = font.kerning
                    override val ascent: Int = font.ascent
                }
            }
        }
    }

    private fun provideSprites(): SpriteProvider {
        val sprites = mutableMapOf<Int, SpriteType>()
        SpriteDecoder().load(cache,sprites)
        return CachedSpriteProvider(sprites)
    }

    fun toIntArrayGray(bytes: ByteArray): IntArray {
        val out = IntArray(bytes.size)

        for (i in bytes.indices) {
            val v = bytes[i].toInt() and 0xFF
            out[i] = (0xFF shl 24) or (v shl 16) or (v shl 8) or v
        }

        return out
    }

    class CachedSpriteProvider(private val archive: MutableMap<Int, SpriteType>) : SpriteProvider {
        override val verdana11PtId: Int = 1442
        override val verdana13ptId: Int = 1445
        override val verdana15ptId: Int = 1447
        fun ByteArray.toIntArray(): IntArray {
            return IntArray(this.size) { i ->
                this[i].toInt() and 0xFF
            }
        }


        override fun getSpriteSheet(id: Int): SpriteSheet? {
            val element = archive[id] ?: return null
            val frames: Array<SpriteFrame> = Array(element.sprites.size) { index ->
                val frame = element.sprites[index]
                object : SpriteFrame {
                    override val xOffset: Int = frame.offsetX
                    override val yOffset: Int = frame.offsetY
                    override val innerWidth: Int = frame.subWidth
                    override val innerHeight: Int = frame.subHeight
                    override val pixels: IntArray = frame.palette
                }
            }
            return object : SpriteSheet {
                override val width: Int = element.sprites.maxBy { it.width }.width
                override val height: Int = element.sprites.maxBy { it.height }.height
                override val frames: Array<SpriteFrame> = frames
            }
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

        return object : ObjectProvider {
            override fun getMapSceneId(id: Int): Int {

                val obj = objects[id]
                obj?.postDecode()
                return obj?.mapSceneID?: -1
            }

            override fun getMapIconId(id: Int): Int {
                val obj = objects[id]
                obj?.postDecode()
                return obj?.mapAreaId?: -1
            }

            override fun getBoundaryType(id: Int): Int {
                val obj = objects[id]
                obj?.postDecode()
                return obj?.interactive?: -1
            }
        }
    }

    class CachedMapProvider(gameCache : dev.openrune.filesystem.Cache) : MapProvider {
        data class Loc(override val id: Int, override val shape: Int, override val rotation: Int, override val coordinate: Coordinate) : WorldMapObject
        private val cache = mutableMapOf<MapsquareId, Mapsquare?>()

        fun storeBuf(mapsquareId: MapsquareId, mapBuffer: ByteBuf, locBuffer: ByteBuf?): Mapsquare {
            val fullMap = FullMapDefinition.decode(mapBuffer, mapsquareId.x, mapsquareId.y)
            val mapObjects = locBuffer?.let { MapLocDefinition.decodeBaseData(it) } ?: emptyList()

            mapBuffer.release()
            locBuffer?.release()
            val worldMapObjects = mapObjects.map { loc ->
                Loc(loc.id, loc.type, loc.orientation, loc.coordinate)
            }
            println("Loaded ${worldMapObjects.size} objects for ${mapsquareId.x},${mapsquareId.y}")
            // Same thing here..
            val land = object : Landscape {
                override fun getUnderlayId(level: Int, x: Int, y: Int): Int {
                    return fullMap.underlayIds[level][x][y] - 1
                }

                override fun getOverlayId(level: Int, x: Int, y: Int): Int {
                    println(fullMap.overlayIds[level][x][y] - 1)
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

        override fun getMap(cache: org.openrs2.cache.Cache, mapsquareX: Int, mapsquareY: Int): Mapsquare? {
            val mapsquareId = MapsquareId(mapsquareX, mapsquareY)
            val cached = this@CachedMapProvider.cache[mapsquareId]
            if (cached != null) return cached
            val (mapBuffer, locBuffer) = readMapBuffers(cache,mapsquareX, mapsquareY) ?: return null
            return storeBuf(mapsquareId, mapBuffer, locBuffer)
        }

        private fun readMapBuffers(cacheProvider : org.openrs2.cache.Cache,mapsquareX: Int, mapsquareY: Int): Pair<ByteBuf, ByteBuf?>? {
            val mapGroupName = "m${mapsquareX}_$mapsquareY"
            val locGroupName = "l${mapsquareX}_$mapsquareY"
            val groupId = (mapsquareX shl 8) or (mapsquareY and 0xFF)
            if (!cacheProvider.exists(MAPS, groupId,0)) return null
            val mapFile = try {
                cacheProvider.read(MAPS, groupId, 0)
            } catch (e: IOException) {
                return null
            }
            val locFile = try {
                if (!cacheProvider.exists(MAPS, groupId,1)) null else cacheProvider.read(MAPS, groupId, 0)
            } catch (e: IOException) {
                null
            }
            return mapFile to locFile
        }
    }

    private fun provideCache(cacheWriter: Path): CacheProvider {
        val cache = org.openrs2.cache.Cache.open(cacheWriter)
        return object : CacheProvider() {
            override fun read(archive: Int, group: Int, file: Int): ByteBuf {
                return cache.read(archive, group, file)
            }

            override fun read(archive: Int, group: String, file: String): ByteBuf {
                return cache.read(archive, group, file)
            }

            override fun exists(archive: Int, group: Int, file: Int): Boolean {
                return cache.exists(archive, group, file)
            }

            override fun exists(archive: Int, group: String, file: String): Boolean {
                return cache.exists(archive, group, file)
            }

            override fun write(archive: Int, group: Int, file: Int, buf: ByteBuf) {
                require(buf.isReadable)
                cache.write(archive, group, file, buf)
            }

            override fun write(archive: Int, group: String, file: String, buf: ByteBuf) {
                require(buf.isReadable)
                cache.write(archive, group, file, buf)
            }

            override fun write(archive: Int, group: String, file: String, fileId: Int, buf: ByteBuf) {
                require(buf.isReadable)
                error("TODO THIS")
                //cache.write(archive, group, file, fileId, buf)
            }

            override fun list(archive: Int): List<Int> {
                return cache.list(archive).asSequence().map { it.id }.toList()
            }

            override fun list(archive: Int, group: Int): List<Int> {
                return cache.list(archive, group).asSequence().map { it.id }.toList()
            }

            override fun list(archive: Int, group: String): List<Int> {
                return cache.list(archive, group).asSequence().map { it.id }.toList()
            }
        }
    }
}

