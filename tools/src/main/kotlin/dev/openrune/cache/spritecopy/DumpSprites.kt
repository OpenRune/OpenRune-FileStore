package dev.openrune.cache.spritecopy

import dev.openrune.cache.filestore.definition.SpriteDecoder
import dev.openrune.definition.type.SpriteSaveMode
import dev.openrune.definition.type.SpriteType
import dev.openrune.filesystem.Cache
import java.nio.file.Path

object DumpSprites {
    @JvmStatic
    fun main(args: Array<String>) {
        val sourceDir = "C:\\Users\\Advo\\Downloads\\rev cache diff\\revitalize"
        val cache = Cache.load(Path.of(sourceDir))

        val sprites: MutableMap<Int, SpriteType> = mutableMapOf()

        SpriteDecoder().load(cache, sprites)

        SpriteType.dumpAllSprites(
            sprites = sprites,
            spriteSaveMode = SpriteSaveMode.SPRITE_SHEET
        )
    }
}