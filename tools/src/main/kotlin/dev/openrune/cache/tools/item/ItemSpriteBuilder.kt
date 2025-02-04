package dev.openrune.cache.tools.item

import java.awt.image.BufferedImage

class ItemSpriteBuilder(private val itemSpriteFactory : ItemSpriteFactory, val itemID: Int) {

    var quantity: Int = 1
    var size: Int = 36
    var border: Int = 1
    var shadowColor: Int = 3153952
    var xan2d: Int = -1
    var zoom2d: Int = -1
    var yan2d: Int = -1
    var zan2d: Int = -1
    var xOffset2d: Int = -1
    var yOffset2d: Int = -1
        

    // Builder methods
    fun quantity(quantity: Int) = apply { this.quantity = quantity }
    fun size(size: Int) = apply { this.size = size }
    fun border(border: Int) = apply { this.border = border }
    fun shadowColor(shadowColor: Int) = apply { this.shadowColor = shadowColor }
    fun xan2d(xan2d: Int) = apply { this.xan2d = xan2d }
    fun zoom2d(zoom2d: Int) = apply { this.zoom2d = zoom2d }
    fun yan2d(yan2d: Int) = apply { this.yan2d = yan2d }
    fun zan2d(zan2d: Int) = apply { this.zan2d = zan2d }
    fun xOffset2d(xOffset2d: Int) = apply { this.xOffset2d = xOffset2d }
    fun yOffset2d(yOffset2d: Int) = apply { this.yOffset2d = yOffset2d }

    fun create(): BufferedImage? {
        return itemSpriteFactory.createSprite(this)
    }
}