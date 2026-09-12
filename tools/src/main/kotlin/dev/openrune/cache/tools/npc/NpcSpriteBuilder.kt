package dev.openrune.cache.tools.npc

import java.awt.image.BufferedImage

class NpcSpriteBuilder(private val factory: NpcSpriteFactory, val npcId: Int) {
    var size = 128
    var border = 0
    var shadowColor = 0
    var orientation = 0
    var yan = 0
    var xan = 160
    var zan = 0
    var chathead = false
    var fitToCanvas = true
    var fitMargin = 0.05
    var cameraDistance = 8.0

    fun size(size: Int): NpcSpriteBuilder = apply { this.size = size }

    fun border(border: Int): NpcSpriteBuilder = apply { this.border = border }

    fun shadowColor(shadowColor: Int): NpcSpriteBuilder = apply { this.shadowColor = shadowColor }

    fun orientation(orientation: Int): NpcSpriteBuilder = apply { this.orientation = orientation }

    fun yan(yan: Int): NpcSpriteBuilder = apply { this.yan = yan }

    fun xan(xan: Int): NpcSpriteBuilder = apply { this.xan = xan }

    fun zan(zan: Int): NpcSpriteBuilder = apply { this.zan = zan }

    fun chathead(chathead: Boolean): NpcSpriteBuilder = apply { this.chathead = chathead }

    fun fitToCanvas(fit: Boolean): NpcSpriteBuilder = apply { this.fitToCanvas = fit }

    fun fitMargin(margin: Double): NpcSpriteBuilder = apply { this.fitMargin = margin }

    fun cameraDistance(distance: Double): NpcSpriteBuilder = apply { this.cameraDistance = distance }

    fun create(): BufferedImage? = factory.createSprite(this)
}
