package dev.openrune.definition.game.render.draw

open class Rasterizer2D {

    lateinit var graphicsPixels: IntArray
    var graphicsPixelsWidth: Int = 0
    var graphicsPixelsHeight: Int = 0
    var drawingAreaTop: Int = 0
    var drawingAreaBottom: Int = 0
    var drawRegionX: Int = 0
    var drawingAreaRight: Int = 0

    fun setRasterBuffer(buffer: IntArray, width: Int, height: Int) {
        graphicsPixels = buffer
        graphicsPixelsWidth = width
        graphicsPixelsHeight = height
        setDrawRegion(0, 0, width, height)
    }

    fun setDrawRegion(x: Int, y: Int, right: Int, bottom: Int) {
        drawRegionX = x.coerceAtLeast(0)
        drawingAreaTop = y.coerceAtLeast(0)
        drawingAreaRight = right.coerceAtMost(graphicsPixelsWidth)
        drawingAreaBottom = bottom.coerceAtMost(graphicsPixelsHeight)
    }

    fun reset() {
        graphicsPixels.fill(0)
    }
}
