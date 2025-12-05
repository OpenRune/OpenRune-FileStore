package dev.openrune.cache.filestore.definition

import dev.openrune.cache.FONTS
import dev.openrune.cache.MODELS
import dev.openrune.cache.SPRITES
import dev.openrune.definition.codec.FontCodec
import dev.openrune.definition.codec.ModelCodec
import dev.openrune.definition.type.FontType
import dev.openrune.definition.type.model.MeshDecodingOption
import dev.openrune.definition.type.model.ModelType
import dev.openrune.filesystem.Cache
import dev.openrune.definition.util.decompressGzip
import io.netty.buffer.Unpooled

class FontDecoder(private val cache: Cache) {

    fun loadAllFonts(): MutableMap<Int, FontType> {
        val fonts = mutableMapOf<Int, FontType>()

        cache.archives(FONTS).forEach { archive ->
            val spriteData = cache.data(SPRITES, archive, 0, null) ?: return@forEach
            SpriteData.decode(spriteData)

            val codec = FontCodec(
                SpriteData.xOffsets,
                SpriteData.yOffsets,
                SpriteData.spriteWidths,
                SpriteData.spriteHeights,
                SpriteData.spritePalette,
                SpriteData.pixels
            )

            val fontData = cache.data(FONTS, archive, 0, null) ?: return@forEach
            fonts[archive] = codec.loadData(archive,fontData)

            // Reset SpriteData arrays after usage
            SpriteData.xOffsets = intArrayOf()
            SpriteData.yOffsets = intArrayOf()
            SpriteData.spriteWidths = intArrayOf()
            SpriteData.spriteHeights = intArrayOf()
            SpriteData.spritePalette = intArrayOf()
            SpriteData.pixels = arrayOf()
        }

        return fonts
    }
}