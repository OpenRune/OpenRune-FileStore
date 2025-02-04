package dev.openrune.cache.tools.item

import dev.openrune.cache.filestore.definition.ModelDecoder
import dev.openrune.definition.game.*
import dev.openrune.definition.game.render.draw.Rasterizer3D
import dev.openrune.definition.game.render.draw.SpritePixels
import dev.openrune.definition.game.render.model.Model
import dev.openrune.definition.game.render.util.JagexColor
import dev.openrune.definition.type.ItemType
import dev.openrune.definition.type.SpriteType
import dev.openrune.definition.type.TextureType
import java.awt.image.BufferedImage
import java.io.IOException


class ItemSpriteFactory(
    private val modelDecoder: ModelDecoder,
    private val textures: MutableMap<Int, TextureType>,
    private val sprites1: MutableMap<Int, SpriteType>,
    val items: Map<Int, ItemType>
) {

    lateinit var itemSpriteFactory : ItemSpriteBuilder

    @Throws(IOException::class)
    fun createSprite(itemSpriteFactory: ItemSpriteBuilder): BufferedImage? {
        this.itemSpriteFactory =  itemSpriteFactory
        val spritePixels = createSpritePixels(
            itemSpriteFactory.itemID,
            itemSpriteFactory.quantity,
            itemSpriteFactory.border,
            itemSpriteFactory.shadowColor,
            false
        )
        return spritePixels?.toBufferedImage()
    }


    private fun linkItem(item: ItemType) {
        item.noteTemplateId.takeIf { it != -1 }?.let {
            item.linkNote(items[it]!!, items[item.noteLinkId]!!)
        }
        item.notedId.takeIf { it != -1 }?.let {
            item.linkBought(items[it]!!, items[item.unnotedId]!!)
        }
        item.placeholderTemplate.takeIf { it != -1 }?.let {
            item.linkPlaceholder(items[it]!!, items[item.placeholderLink]!!)
        }
    }

    @Throws(IOException::class)
    private fun createSpritePixels(
        itemId: Int,
        quantity: Int,
        border: Int,
        shadowColor: Int,
        noted: Boolean
    ): SpritePixels? {
        var item = items[itemId] ?: return null

        linkItem(item)

        if (quantity > 1 && item.countObj != null) {
            var stackItemID = -1

            for (i in 0..9) {
                if (quantity >= item.countCo!![i] && item.countCo!![i] != 0) {
                    stackItemID = item.countObj!![i]
                }
            }

            if (stackItemID != -1) {
                item = items[stackItemID] ?: return null
            }
        }

        val itemModel = getModel(item) ?: return null

        val auxSpritePixels = createAuxSpritePixels(item)

        val spritePixels = SpritePixels(itemSpriteFactory.size, itemSpriteFactory.size)

        val graphics = Rasterizer3D(textures, sprites1).apply {
            setBrightness(JagexColor.BRIGHTNESS_MAX)
            setRasterBuffer(spritePixels.pixels, 36, 32)
            reset()
            setRasterClipping()
            setOffset(16, 16)
            isGouraudShadingLowRes = false
        }

        auxSpritePixels?.drawAtOn(graphics, 0, 0)

        val zoom = calculateZoom(item, border, noted)

        drawItemModel(itemModel, item, graphics, zoom)

        applyBordersAndShadows(spritePixels, border, shadowColor)

        graphics.setRasterBuffer(spritePixels.pixels, itemSpriteFactory.size, itemSpriteFactory.size)
        auxSpritePixels?.drawAtOn(graphics, 0, 0)

        return spritePixels
    }

    private fun calculateZoom(item: ItemType, border: Int, noted: Boolean): Int {
        val zoom2d = if (itemSpriteFactory.zoom2d == -1) item.zoom2d else itemSpriteFactory.zoom2d
        var zoom = zoom2d
        zoom = when {
            noted -> (zoom * 1.5).toInt()
            border == 2 -> (zoom * 1.04).toInt()
            else -> zoom
        }
        return zoom
    }

    private fun drawItemModel(itemModel: Model, item: ItemType, graphics: Rasterizer3D, zoom: Int) {

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
            0,
            yan2d,
            zan2d,
            xan2d,
            xOffset2d,
            itemModel.modelHeight / 2 + var17 + yOffset2d,
            var18 + yOffset2d
        )
    }

    private fun createAuxSpritePixels(item: ItemType): SpritePixels? {
        return when {
            item.noteTemplateId != -1  -> createSpritePixels(item.noteLinkId, 10, 1, 0, true)
            item.notedId != -1 -> createSpritePixels(item.unnotedId, item.countObj?.size ?: 0, 1, 0, false)
            item.placeholderTemplate != -1 -> createSpritePixels(
                item.placeholderLink,
                item.countObj?.size ?: 0,
                0,
                0,
                false
            )

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

        return inventoryModel.toModel(item.ambient + 64, item.contrast + 768, -50, -10, -50)
    }

    private fun applyBordersAndShadows(spritePixels: SpritePixels, border: Int, shadowColor: Int) {
        if (border >= 1) spritePixels.drawBorder(1)
        if (border >= 2) spritePixels.drawBorder(0xffffff)
        if (shadowColor != 0) spritePixels.drawShadow(shadowColor)
    }

}
