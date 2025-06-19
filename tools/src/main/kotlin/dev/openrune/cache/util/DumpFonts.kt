package dev.openrune.cache.util

import com.google.gson.GsonBuilder
import dev.openrune.cache.filestore.definition.FontDecoder
import dev.openrune.cache.gameval.GameValHandler
import dev.openrune.cache.gameval.GameValHandler.lookup
import dev.openrune.definition.GameValGroupTypes
import dev.openrune.definition.type.FontType
import dev.openrune.filesystem.Cache
import java.io.File

data class FontJson(
    val ascent: Int,
    val maxAscent: Int,
    val maxDescent: Int,
    val glyphs: List<Glyph>
) {
    data class Glyph(
        val codePoint: Int,
        val topBearing: Int,
        val height: Int,
        val width: Int,
        val advance: Int,
        val leftBearing: Int,
        val pixels: ByteArray
    )
}

class DumpFonts(private val cache: Cache, private val dumpLocation: File) {

    fun init() {

        val spriteNames = GameValHandler.readGameVal(GameValGroupTypes.SPRITETYPES,cache)

        val fontDecoder = FontDecoder(cache)

        fontDecoder.loadAllFonts().forEach { (key, font) ->
            val name = spriteNames.lookup(key)?.name?: ""
            generateJson(name, font)
        }
    }

    private fun generateJson(name: String, font: FontType) {
        val glyphs = (32 until 256).mapNotNull { i ->
            val topBearing = font.topBearings[i]
            val height = font.heights[i]
            val width = font.widths[i]
            val advance = font.advances[i]
            val leftBearing = font.leftBearings[i]
            val pixels = font.pixels[i]

            val isEmptyPixels = pixels.isEmpty()
            val allDefault = topBearing == 0 && height == 0 && width == 0 && advance == 0 && leftBearing == 0 && isEmptyPixels

            if (allDefault) null
            else FontJson.Glyph(i, topBearing, height, width, advance, leftBearing, pixels)
        }

        val fontJson = FontJson(font.ascent, font.maxAscent, font.maxDescent, glyphs)

        val json = GsonBuilder()
            .setPrettyPrinting()
            .create()
            .toJson(fontJson)

        File(dumpLocation, "$name.json").writeText(json)
    }
}
