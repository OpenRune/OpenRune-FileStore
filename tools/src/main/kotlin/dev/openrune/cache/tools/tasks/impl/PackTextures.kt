package dev.openrune.cache.tools.tasks.impl

import cc.ekblad.toml.decode
import cc.ekblad.toml.tomlMapper
import com.displee.cache.CacheLibrary
import dev.openrune.buffer.toArray
import dev.openrune.cache.SPRITES
import dev.openrune.cache.TEXTURES
import dev.openrune.definition.type.TextureType
import dev.openrune.cache.tools.tasks.CacheTask
import dev.openrune.cache.tools.tasks.impl.PackSprites.Companion.customSprites
import dev.openrune.cache.tools.tasks.impl.defs.PackConfig.Companion.mergeDefinitions
import dev.openrune.cache.tools.tasks.impl.sprites.SpriteSet
import dev.openrune.cache.tools.tasks.impl.sprites.SpriteSet.Companion.averageColorForPixels
import dev.openrune.cache.util.getFiles
import dev.openrune.cache.util.progress
import dev.openrune.definition.codec.TextureCodec
import io.github.oshai.kotlinlogging.KotlinLogging
import io.netty.buffer.Unpooled
import java.io.File

class PackTextures(private val textureDir : File) : CacheTask() {

    private val logger = KotlinLogging.logger {}

    private val mapper = tomlMapper {}

    override fun init(library: CacheLibrary) {
        val size = getFiles(textureDir,"toml").size
        val progress = progress("Packing Textures", size)

        if (size != 0) {

            getFiles(textureDir,"toml").forEach {

                var def = mapper.decode<TextureType>(it.toPath())

                val defId = def.id

                if (def.inherit != -1) {
                    val data = library.data(TEXTURES, 0,def.inherit)
                    data.let {
                        val inheritedDef = TextureCodec().loadData(def.inherit,data!!)
                        def = mergeDefinitions(inheritedDef, def)
                    }
                }

                if (def.fileIds.isNotEmpty() && defId != -1) {
                    val spriteID = def.fileIds.first()
                    if (customSprites.containsKey(spriteID)) {
                        def.averageRgb = customSprites[spriteID]?.averageColor ?: 0
                    } else {
                        val sprite = SpriteSet.decode(spriteID, Unpooled.wrappedBuffer(library.data(SPRITES, spriteID))).sprites.first()
                        def.averageRgb = averageColorForPixels(sprite.image)
                    }
                    val encoder = TextureCodec()
                    val writer = Unpooled.buffer(4096)
                    with(encoder) { writer.encode(def) }

                    library.put(TEXTURES,0,defId, writer.toArray())
                    progress.step()
                } else {
                    logger.info { "Unable to Pack Texture ID is -1 or no fileIds has been defined" }
                }
            }

            progress.close()

        }
    }

}