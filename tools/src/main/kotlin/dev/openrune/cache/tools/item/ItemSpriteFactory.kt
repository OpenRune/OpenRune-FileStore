package dev.openrune.cache.tools.item

import dev.openrune.OsrsCacheProvider
import dev.openrune.cache.filestore.definition.ModelDecoder
import dev.openrune.cache.filestore.definition.SpriteDecoder
import dev.openrune.definition.game.*
import dev.openrune.definition.game.render.draw.Rasterizer3D
import dev.openrune.definition.game.render.draw.SpritePixels
import dev.openrune.definition.game.render.model.Model
import dev.openrune.definition.game.render.model.ModelContext
import dev.openrune.definition.game.render.util.JagexColor
import dev.openrune.definition.type.ItemType
import dev.openrune.definition.type.SpriteType
import dev.openrune.definition.type.TextureType
import dev.openrune.filesystem.Cache
import readCacheRevision
import java.awt.image.BufferedImage
import java.io.IOException
import kotlin.math.roundToInt
import kotlin.math.sqrt

private val ItemType.hasTemplate: Boolean
    get() = noteTemplateId != -1 || notedId != -1 || placeholderTemplate != -1

class ItemSpriteFactory(
    private val modelDecoder: ModelDecoder,
    private val textures: MutableMap<Int, TextureType>,
    private val sprites1: MutableMap<Int, SpriteType>,
    val items: Map<Int, ItemType>
) {
    companion object {
        private const val CLIENT_FRAME_HEIGHT = 32

        private const val CLIENT_FRAME_WIDTH = 36

        private const val CLIENT_PROJECTION_ZOOM = 512

        private const val MAX_PROJECTION_ZOOM = 10_000

        private const val BACKGROUND_BLACK = 0x000000
        private const val BACKGROUND_WHITE = 0xffffff

        @JvmStatic
        @JvmOverloads
        fun fromCache(
            cache: Cache,
            revision: Int = readCacheRevision(cache),
            items: Map<Int, ItemType>? = null
        ): ItemSpriteFactory {
            val loadedItems = items ?: mutableMapOf<Int, ItemType>().also {
                OsrsCacheProvider.ItemDecoder(revision).load(cache, it)
            }
            val textures = mutableMapOf<Int, TextureType>().also {
                OsrsCacheProvider.TextureDecoder(revision).load(cache, it)
            }
            val sprites = mutableMapOf<Int, SpriteType>().also { SpriteDecoder().load(cache, it) }
            return ItemSpriteFactory(ModelDecoder(cache), textures, sprites, loadedItems)
        }
    }

    fun item(itemID: Int): ItemSpriteBuilder = ItemSpriteBuilder(this, itemID)

    lateinit var itemSpriteFactory : ItemSpriteBuilder

    private var canvasSize = 0

    private var projectionZoom = CLIENT_PROJECTION_ZOOM

    private var usedTemplate = false

    @Throws(IOException::class)
    fun createSprite(itemSpriteFactory: ItemSpriteBuilder): BufferedImage? {
        this.itemSpriteFactory = itemSpriteFactory
        canvasSize = itemSpriteFactory.size
        projectionZoom = projectionZoomFor(canvasSize)
        return try {
            usedTemplate = false
            val overBlack = render(BACKGROUND_BLACK, decorate = false) ?: return null

            // Templates composite with a zero-means-empty mask, so the white pass would paint
            // the template's background over the item. Those keep the old single-pass path.
            val sprite = if (usedTemplate) {
                render(BACKGROUND_BLACK, decorate = true) ?: return null
            } else {
                val overWhite = render(BACKGROUND_WHITE, decorate = false) ?: return null
                SpritePixels.fromOpaquePasses(overBlack, overWhite).also {
                    applyBordersAndShadows(it, itemSpriteFactory.border, itemSpriteFactory.shadowColor)
                }
            }

            if (itemSpriteFactory.fitToCanvas) sprite.centerContent()
            sprite.toBufferedImage()
        } finally {
            canvasSize = 0
            projectionZoom = CLIENT_PROJECTION_ZOOM
            usedTemplate = false
        }
    }

    private fun projectionZoomFor(size: Int): Int {
        if (size <= 0) return CLIENT_PROJECTION_ZOOM

        val frame = when {
            itemSpriteFactory.fitToCanvas -> CLIENT_FRAME_WIDTH
            itemSpriteFactory.autoScaleZoom -> CLIENT_FRAME_HEIGHT
            else -> return CLIENT_PROJECTION_ZOOM
        }

        val margin = if (itemSpriteFactory.fitToCanvas) itemSpriteFactory.fitMargin.coerceIn(0.0, 0.49) else 0.0
        val scaled = CLIENT_PROJECTION_ZOOM.toDouble() * size / frame * (1.0 - 2.0 * margin)
        return scaled.toInt().coerceIn(1, MAX_PROJECTION_ZOOM)
    }

    private fun render(background: Int, decorate: Boolean): SpritePixels? = createSpritePixels(
        itemSpriteFactory.itemID,
        itemSpriteFactory.quantity,
        itemSpriteFactory.border,
        itemSpriteFactory.shadowColor,
        false,
        background,
        decorate
    )

    private fun linkItem(item: ItemType) {
        if (item.noteTemplateId != -1) {
            val template = items[item.noteTemplateId] ?: return
            val linked = items[item.noteLinkId] ?: return
            item.linkNote(template, linked)
        }
        if (item.notedId != -1) {
            val template = items[item.notedId] ?: return
            val linked = items[item.unnotedId] ?: return
            item.linkBought(template, linked)
        }
        if (item.placeholderTemplate != -1) {
            val template = items[item.placeholderTemplate] ?: return
            val linked = items[item.placeholderLink] ?: return
            item.linkPlaceholder(template, linked)
        }
    }

    @Throws(IOException::class)
    private fun createSpritePixels(
        itemId: Int,
        quantity: Int,
        border: Int,
        shadowColor: Int,
        noted: Boolean,
        background: Int = BACKGROUND_BLACK,
        decorate: Boolean = true
    ): SpritePixels? {
        var item = items[itemId]?.copy() ?: return null

        linkItem(item)

        if (quantity > 1 && item.countObj != null) {
            var stackItemID = -1

            for (i in 0..9) {
                if (quantity >= item.countCo!![i] && item.countCo!![i] != 0) {
                    stackItemID = item.countObj!![i]
                }
            }

            if (stackItemID != -1) {
                item = items[stackItemID]?.copy() ?: return null
                linkItem(item)
            }
        }

        val itemModel = getModel(item) ?: return null

        if (item.hasTemplate) usedTemplate = true

        val auxSpritePixels = createAuxSpritePixels(item, quantity, border)
        if (auxSpritePixels == null && item.hasTemplate) return null

        val spritePixels = SpritePixels(canvasSize, canvasSize)

        val centered = itemSpriteFactory.autoScaleZoom || itemSpriteFactory.fitToCanvas
        val offset = if (centered) canvasSize / 2 else 16

        val graphics = Rasterizer3D(textures, sprites1).apply {
            setBrightness(JagexColor.BRIGHTNESS_MAX)
            setRasterBuffer(spritePixels.pixels, canvasSize, canvasSize)
            reset()
            setRasterClipping()
            setOffset(offset, offset)
            isGouraudShadingLowRes = false
            zoom = projectionZoom
        }
        spritePixels.pixels.fill(background)

        if (item.placeholderTemplate != -1) auxSpritePixels?.drawAtOn(graphics, 0, 0)

        val zoom = calculateZoom(item, border, noted)

        drawItemModel(itemModel, item, graphics, ModelContext(), zoom)

        if (item.notedId != -1) auxSpritePixels?.drawAtOn(graphics, 0, 0)

        if (decorate) applyBordersAndShadows(spritePixels, border, shadowColor)

        if (item.noteTemplateId != -1) {
            graphics.setRasterBuffer(spritePixels.pixels, canvasSize, canvasSize)
            auxSpritePixels?.drawAtOn(graphics, 0, 0)
        }

        return spritePixels
    }

    private fun calculateZoom(item: ItemType, border: Int, noted: Boolean): Int {
        val zoom = if (itemSpriteFactory.zoom2d == -1) item.zoom2d else itemSpriteFactory.zoom2d
        return when {
            noted -> (zoom * 1.5).toInt()
            border == 2 -> (zoom * 1.04).toInt()
            else -> zoom
        }
    }

    private fun drawItemModel(itemModel: Model, item: ItemType, graphics: Rasterizer3D, context: ModelContext, zoom: Int) {
        val yan2d = if (itemSpriteFactory.yan2d == -1) item.yan2d else itemSpriteFactory.yan2d
        val zan2d = if (itemSpriteFactory.zan2d == -1) item.zan2d else itemSpriteFactory.zan2d
        val xan2d = if (itemSpriteFactory.xan2d == -1) item.xan2d else itemSpriteFactory.xan2d
        val yOffset2d = if (itemSpriteFactory.yOffset2d == -1) item.yOffset2d else itemSpriteFactory.yOffset2d
        val xOffset2d = if (itemSpriteFactory.xOffset2d == -1) item.xOffset2d else itemSpriteFactory.xOffset2d

        val var17 = zoom * Rasterizer3D.SINE[xan2d] shr 16
        val var18 = zoom * Rasterizer3D.COSINE[xan2d] shr 16

        itemModel.calculateBoundsCylinder()
        itemModel.projectAndDraw(
            graphics,
            context,
            0,
            yan2d,
            zan2d,
            xan2d,
            xOffset2d,
            itemModel.modelHeight / 2 + var17 + yOffset2d,
            var18 + yOffset2d
        )
    }

    private fun createAuxSpritePixels(item: ItemType, quantity: Int, border: Int): SpritePixels? {
        return when {
            item.noteTemplateId != -1 -> createSpritePixels(item.noteLinkId, 10, 1, 0, true)
            item.notedId != -1 -> createSpritePixels(item.unnotedId, quantity, border, 0, false)
            item.placeholderTemplate != -1 -> createSpritePixels(item.placeholderLink, quantity, 0, 0, false)
            else -> null
        }
    }

    @Throws(IOException::class)
    private fun getModel(item: ItemType): Model? {
        val inventoryModel = modelDecoder.getModel(item.inventoryModel) ?: return null

        inventoryModel.apply {
            if (item.resizeX != 128 || item.resizeY != 128 || item.resizeZ != 128) {
                resize(item.resizeX, item.resizeY, item.resizeZ)
            }

            item.originalColours?.zip(item.modifiedColours.orEmpty())?.forEach { (original, modified) ->
                recolor(original.toShort(), modified.toShort())
            }

            item.originalTextureColours?.zip(item.modifiedTextureColours.orEmpty())?.forEach { (original, modified) ->
                retexture(original.toShort(), modified.toShort())
            }
        }

        return inventoryModel.toModel(item.ambient + 64, item.contrast * 5 + 768, -50, -10, -50)
    }

    private fun applyBordersAndShadows(spritePixels: SpritePixels, border: Int, shadowColor: Int) {
        val scale = outlineScale()
        if (border >= 1) spritePixels.drawBorder(1, scale)
        if (border >= 2) spritePixels.drawBorder(0xffffff, scale)
        if (shadowColor != 0) spritePixels.drawShadow(shadowColor, scale)
    }

    private fun outlineScale(): Int =
        sqrt(projectionZoom.toDouble() / CLIENT_PROJECTION_ZOOM).roundToInt().coerceAtLeast(1)
}
