package dev.openrune.cache.tools.npc

import dev.openrune.OsrsCacheProvider
import dev.openrune.cache.filestore.definition.ModelDecoder
import dev.openrune.cache.filestore.definition.SpriteDecoder
import dev.openrune.definition.game.render.draw.Rasterizer3D
import dev.openrune.definition.game.render.draw.SpritePixels
import dev.openrune.definition.game.render.model.Model
import dev.openrune.definition.game.render.model.ModelContext
import dev.openrune.definition.game.render.util.JagexColor
import dev.openrune.definition.type.NpcType
import dev.openrune.definition.type.SpriteType
import dev.openrune.definition.type.TextureType
import dev.openrune.definition.type.model.ModelType
import dev.openrune.filesystem.Cache
import readCacheRevision
import java.awt.image.BufferedImage
import java.io.IOException
import kotlin.math.roundToInt
import kotlin.math.sqrt

class NpcSpriteFactory(
    private val modelDecoder: ModelDecoder,
    private val textures: MutableMap<Int, TextureType>,
    private val sprites: MutableMap<Int, SpriteType>,
    val npcs: Map<Int, NpcType>
) {
    companion object {
        private const val AMBIENT_BASE = 64
        private const val CONTRAST_BASE = 850

        private const val CONTRAST_SCALE = 5
        private const val LIGHT_X = -30
        private const val LIGHT_Y = -50
        private const val LIGHT_Z = -30

        private const val BASE_PROJECTION_ZOOM = 512
        private const val BASE_FRAME = 36

        private const val MAX_PROJECTION_ZOOM = 10_000

        private const val QUARTER_TURN = 512
        private const val ROTATION_MASK = 2047

        @JvmStatic
        @JvmOverloads
        fun fromCache(
            cache: Cache,
            revision: Int = readCacheRevision(cache),
            npcs: Map<Int, NpcType>? = null
        ): NpcSpriteFactory {
            val loadedNpcs = npcs ?: mutableMapOf<Int, NpcType>().also {
                OsrsCacheProvider.NPCDecoder(revision).load(cache, it)
            }
            val textures = mutableMapOf<Int, TextureType>().also {
                OsrsCacheProvider.TextureDecoder(revision).load(cache, it)
            }
            val sprites = mutableMapOf<Int, SpriteType>().also { SpriteDecoder().load(cache, it) }
            return NpcSpriteFactory(ModelDecoder(cache), textures, sprites, loadedNpcs)
        }
    }

    fun npc(npcId: Int): NpcSpriteBuilder = NpcSpriteBuilder(this, npcId)

    @Throws(IOException::class)
    fun createSprite(builder: NpcSpriteBuilder): BufferedImage? {
        val npc = resolve(builder.npcId) ?: return null
        val mesh = buildMesh(npc, builder.chathead) ?: return null

        val bounds = MeshBounds.of(mesh)
        if (bounds.extent <= 0) return null

        val model = mesh.toModel(
            npc.ambient + AMBIENT_BASE,
            npc.contrast * CONTRAST_SCALE + CONTRAST_BASE,
            LIGHT_X,
            LIGHT_Y,
            LIGHT_Z
        )

        val size = builder.size
        val distance = (bounds.extent * builder.cameraDistance).roundToInt().coerceAtLeast(1)
        val projectionZoom = projectionZoom(builder, distance, bounds.extent)

        val spritePixels = SpritePixels(size, size)
        val graphics = Rasterizer3D(textures, sprites).apply {
            setBrightness(JagexColor.BRIGHTNESS_MAX)
            setRasterBuffer(spritePixels.pixels, size, size)
            reset()
            setRasterClipping()
            setOffset(size / 2, size / 2)
            isGouraudShadingLowRes = false
            zoom = projectionZoom
        }

        draw(model, graphics, builder, distance)

        if (spritePixels.contentBounds() == null) return null

        val outline = outlineScale(projectionZoom)
        if (builder.border >= 1) spritePixels.drawBorder(1, outline)
        if (builder.border >= 2) spritePixels.drawBorder(0xffffff, outline)
        if (builder.shadowColor != 0) spritePixels.drawShadow(builder.shadowColor, outline)

        if (builder.fitToCanvas) spritePixels.centerContent()
        return spritePixels.toBufferedImage()
    }

    private fun resolve(npcId: Int): NpcType? {
        val npc = npcs[npcId] ?: return null
        if (npc.models?.isNotEmpty() == true) return npc

        val branches = npc.transforms ?: return npc
        val target = npc.multiDefault.takeIf { it != -1 && npcs[it]?.models?.isNotEmpty() == true }
            ?: branches.firstOrNull { it != -1 && npcs[it]?.models?.isNotEmpty() == true }
        return target?.let { npcs[it] } ?: npc
    }

    private fun buildMesh(npc: NpcType, chathead: Boolean): ModelType? {
        val ids = (if (chathead) npc.chatheadModels else npc.models)?.filter { it != -1 } ?: return null
        if (ids.isEmpty()) return null

        val parts = ids.mapNotNull { modelDecoder.getModel(it) }
        if (parts.isEmpty()) return null

        val mesh = if (parts.size == 1) parts.first() else ModelType().apply { append(parts) }

        npc.originalColours?.zip(npc.modifiedColours.orEmpty())?.forEach { (from, to) ->
            mesh.recolor(from.toShort(), to.toShort())
        }
        npc.originalTextureColours?.zip(npc.modifiedTextureColours.orEmpty())?.forEach { (from, to) ->
            mesh.retexture(from.toShort(), to.toShort())
        }

        if (npc.widthScale != 128 || npc.heightScale != 128) {
            mesh.resize(npc.widthScale, npc.heightScale, npc.widthScale)
        }

        return mesh
    }

    private fun draw(model: Model, graphics: Rasterizer3D, builder: NpcSpriteBuilder, distance: Int) {
        val pitchY = distance * Rasterizer3D.SINE[builder.xan] shr 16
        val pitchZ = distance * Rasterizer3D.COSINE[builder.xan] shr 16

        val yan = (builder.yan + builder.orientation * QUARTER_TURN) and ROTATION_MASK

        model.calculateBoundsCylinder()
        model.projectAndDraw(
            graphics,
            ModelContext(),
            0,
            yan,
            builder.zan,
            builder.xan,
            0,
            model.modelHeight / 2 + pitchY,
            pitchZ
        )
    }

    private fun projectionZoom(builder: NpcSpriteBuilder, distance: Int, extent: Int): Int {
        if (!builder.fitToCanvas) return BASE_PROJECTION_ZOOM * builder.size / BASE_FRAME

        val fill = 1.0 - 2.0 * builder.fitMargin.coerceIn(0.0, 0.49)
        val zoom = builder.size / 2.0 * fill * distance / extent
        return zoom.roundToInt().coerceIn(1, MAX_PROJECTION_ZOOM)
    }

    private fun outlineScale(projectionZoom: Int): Int =
        sqrt(projectionZoom.toDouble() / BASE_PROJECTION_ZOOM).roundToInt().coerceAtLeast(1)

    private data class MeshBounds(val radius: Int, val height: Int) {
        val extent: Int get() = maxOf(radius, height / 2)

        companion object {
            fun of(mesh: ModelType): MeshBounds {
                val xs = mesh.vertexPositionsX ?: return MeshBounds(0, 0)
                val ys = mesh.vertexPositionsY ?: return MeshBounds(0, 0)
                val zs = mesh.vertexPositionsZ ?: return MeshBounds(0, 0)

                var radiusSquared = 0
                var minY = 0
                var maxY = 0
                for (i in 0 until mesh.vertexCount) {
                    val x = xs[i]
                    val y = ys[i]
                    val z = zs[i]
                    radiusSquared = maxOf(radiusSquared, x * x + z * z)
                    minY = minOf(minY, y)
                    maxY = maxOf(maxY, y)
                }

                return MeshBounds(sqrt(radiusSquared.toDouble()).roundToInt(), maxY - minY)
            }
        }
    }
}
