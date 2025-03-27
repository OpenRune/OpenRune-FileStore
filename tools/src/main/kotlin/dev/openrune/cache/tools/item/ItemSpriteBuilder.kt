package dev.openrune.cache.tools.item

import java.awt.image.BufferedImage

class ItemSpriteBuilder(private val itemSpriteFactory: ItemSpriteFactory, val itemID: Int) {

    var quantity = 1
    var size = 36
    var border = 1
    var shadowColor = 3153952
    var xan2d = -1
    var zoom2d = -1
    var yan2d = -1
    var zan2d = -1
    var xOffset2d = -1
    var yOffset2d = -1
    var autoScaleZoom = false

    fun quantity(quantity: Int): ItemSpriteBuilder {
        this.quantity = quantity
        return this
    }

    /**
     * Sets the size (width and height) of the sprite.
     *
     * @param size The size in pixels.
     * @return The updated builder instance.
     */
    fun size(size: Int): ItemSpriteBuilder {
        this.size = size
        return this
    }

    /**
     * Enables or disables auto-scaling zoom based on the sprite size.
     *
     * @param autoScale Whether auto-scaling should be applied.
     * @return The updated builder instance.
     */
    fun autoScaleZoom(autoScale: Boolean): ItemSpriteBuilder {
        this.autoScaleZoom = autoScale
        return this
    }

    /**
     * Sets the border thickness around the sprite.
     *
     * @param border The border thickness in pixels.
     * @return The updated builder instance.
     */
    fun border(border: Int): ItemSpriteBuilder {
        this.border = border
        return this
    }

    /**
     * Sets the shadow color for the sprite.
     *
     * @param shadowColor The shadow color as an integer.
     * @return The updated builder instance.
     */
    fun shadowColor(shadowColor: Int): ItemSpriteBuilder {
        this.shadowColor = shadowColor
        return this
    }

    /**
     * Sets the 2D X-axis rotation.
     *
     * @param xan2d The X-axis rotation value.
     * @return The updated builder instance.
     */
    fun xan2d(xan2d: Int): ItemSpriteBuilder {
        this.xan2d = xan2d
        return this
    }

    /**
     * Sets the 2D zoom level.
     *
     * @param zoom2d The zoom level.
     * @return The updated builder instance.
     */
    fun zoom2d(zoom2d: Int): ItemSpriteBuilder {
        this.zoom2d = zoom2d
        return this
    }

    /**
     * Sets the 2D Y-axis rotation.
     *
     * @param yan2d The Y-axis rotation value.
     * @return The updated builder instance.
     */
    fun yan2d(yan2d: Int): ItemSpriteBuilder {
        this.yan2d = yan2d
        return this
    }

    /**
     * Sets the 2D Z-axis rotation.
     *
     * @param zan2d The Z-axis rotation value.
     * @return The updated builder instance.
     */
    fun zan2d(zan2d: Int): ItemSpriteBuilder {
        this.zan2d = zan2d
        return this
    }

    /**
     * Sets the X-axis offset.
     *
     * @param xOffset2d The X-axis offset value.
     * @return The updated builder instance.
     */
    fun xOffset2d(xOffset2d: Int): ItemSpriteBuilder {
        this.xOffset2d = xOffset2d
        return this
    }

    /**
     * Sets the Y-axis offset.
     *
     * @param yOffset2d The Y-axis offset value.
     * @return The updated builder instance.
     */
    fun yOffset2d(yOffset2d: Int): ItemSpriteBuilder {
        this.yOffset2d = yOffset2d
        return this
    }

    /**
     * Creates the item sprite with the specified properties.
     *
     * @return A BufferedImage representing the item sprite, or null if creation fails.
     */
    fun create(): BufferedImage? {
        return itemSpriteFactory.createSprite(this)
    }
}