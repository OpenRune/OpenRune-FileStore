package dev.openrune.game.item

import dev.openrune.game.item.ItemSpriteLoader.itemManager
import net.runelite.cache.definitions.ItemDefinition
import net.runelite.cache.item.ItemSpriteFactory
import java.awt.image.BufferedImage

class ItemSpriteBuilder(val itemID: Int) {

    val itemType: ItemDefinition = itemManager.provide(itemID)

    companion object {
        lateinit var itemSpriteFactory: ItemSpriteFactory

        fun init(itemSpriteFactory: ItemSpriteFactory) {
            Companion.itemSpriteFactory = itemSpriteFactory
        }
    }

    var quantity: Int = 1
        private set
    var width: Int = 36
        private set
    var height: Int = 36
        private set
    var border: Int = 1
        private set
    var shadowColor: Int = 3153952
        private set
    var isNoted: Boolean = false
        private set

    var xan2d: Int = itemType.xan2d
        private set
    var zoom2d: Int = itemType.zoom2d
        private set
    var yan2d: Int = itemType.yan2d
        private set
    var zan2d: Int = itemType.zan2d
        private set
    var xOffset2d: Int = itemType.xOffset2d
        private set
    var yOffset2d: Int = itemType.yOffset2d
        private set

    // Builder methods
    fun quantity(quantity: Int) = apply { this.quantity = quantity }
    fun width(width: Int) = apply { this.width = width }
    fun height(height: Int) = apply { this.height = height }
    fun border(border: Int) = apply { this.border = border }
    fun shadowColor(shadowColor: Int) = apply { this.shadowColor = shadowColor }
    fun noted(noted: Boolean) = apply { this.isNoted = noted }
    fun xan2d(xan2d: Int) = apply { this.xan2d = xan2d }
    fun zoom2d(zoom2d: Int) = apply { this.zoom2d = zoom2d }
    fun yan2d(yan2d: Int) = apply { this.yan2d = yan2d }
    fun zan2d(zan2d: Int) = apply { this.zan2d = zan2d }
    fun xOffset2d(xOffset2d: Int) = apply { this.xOffset2d = xOffset2d }
    fun yOffset2d(yOffset2d: Int) = apply { this.yOffset2d = yOffset2d }

    // Copy method
    fun copy(other: ItemSpriteBuilder): ItemSpriteBuilder = apply {
        this.quantity = other.quantity
        this.width = other.width
        this.height = other.height
        this.border = other.border
        this.shadowColor = other.shadowColor
        this.isNoted = other.isNoted
        this.xOffset2d = other.xOffset2d
        this.yOffset2d = other.yOffset2d
    }

    fun build(): BufferedImage {
        xan2d = if (xan2d == -1) itemType.xan2d else xan2d
        zoom2d = if (zoom2d == -1) itemType.zoom2d else zoom2d
        yan2d = if (yan2d == -1) itemType.yan2d else yan2d
        zan2d = if (zan2d == -1) itemType.zan2d else zan2d
        xOffset2d = if (xOffset2d == -1) itemType.xOffset2d else xOffset2d
        yOffset2d = if (yOffset2d == -1) itemType.yOffset2d else yOffset2d

        return itemSpriteFactory.createSprite(this)
    }
}
