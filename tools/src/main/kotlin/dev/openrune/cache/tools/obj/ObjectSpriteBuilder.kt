package dev.openrune.cache.tools.obj

import java.awt.image.BufferedImage

class ObjectSpriteBuilder(private val factory: ObjectSpriteFactory, val objectId: Int) {
    var size = 128
    var border = 0
    var shadowColor = 0
    var orientation = 0
    var yan = 0
    var xan = 160
    var zan = 0
    var shape: Int? = null
    var fitToCanvas = true
    var fitMargin = 0.05
    var cameraDistance = 8.0

    fun size(size: Int): ObjectSpriteBuilder = apply { this.size = size }

    fun border(border: Int): ObjectSpriteBuilder = apply { this.border = border }

    fun shadowColor(shadowColor: Int): ObjectSpriteBuilder = apply { this.shadowColor = shadowColor }

    fun orientation(orientation: Int): ObjectSpriteBuilder = apply { this.orientation = orientation }

    fun yan(yan: Int): ObjectSpriteBuilder = apply { this.yan = yan }

    fun xan(xan: Int): ObjectSpriteBuilder = apply { this.xan = xan }

    fun zan(zan: Int): ObjectSpriteBuilder = apply { this.zan = zan }

    fun shape(shape: Int?): ObjectSpriteBuilder = apply { this.shape = shape }

    fun fitToCanvas(fit: Boolean): ObjectSpriteBuilder = apply { this.fitToCanvas = fit }

    fun fitMargin(margin: Double): ObjectSpriteBuilder = apply { this.fitMargin = margin }

    fun cameraDistance(distance: Double): ObjectSpriteBuilder = apply { this.cameraDistance = distance }

    fun create(): BufferedImage? = factory.createSprite(this)
}
