package dev.openrune.definition.type

import dev.openrune.definition.Definition

class FontType(
    override var id: Int = -1,
    var leftBearings: IntArray,
    var topBearings: IntArray,
    var widths: IntArray,
    var heights: IntArray,
    var pixels: Array<ByteArray>,
): Definition {
    var glyphAdvances: IntArray = emptyArray<Int>().toIntArray()
    var ascent: Int = 0
    var maxAscent: Int = 0
    var maxDescent: Int = 0
    var kerning: ByteArray = emptyArray<Byte>().toByteArray()
}