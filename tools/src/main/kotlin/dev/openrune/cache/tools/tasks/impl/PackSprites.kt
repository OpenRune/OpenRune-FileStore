package dev.openrune.cache.tools.tasks.impl

import com.displee.cache.CacheLibrary
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dev.openrune.cache.SPRITES
import dev.openrune.cache.tools.tasks.CacheTask
import dev.openrune.cache.tools.tasks.impl.sprites.Sprite
import dev.openrune.cache.tools.tasks.impl.sprites.SpriteManifest
import dev.openrune.cache.tools.tasks.impl.sprites.SpriteSet
import dev.openrune.cache.util.getFiles
import dev.openrune.cache.util.progress
import io.netty.buffer.Unpooled
import me.tongfei.progressbar.ProgressBar
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO


class PackSprites(
    private val spritesDirectory: File,
    private val spriteManifest: File = File(spritesDirectory, "manifest.json")
) : CacheTask() {

    private val customSprites: MutableMap<Int, SpriteSet> = mutableMapOf()
    private var manifest: Map<String, SpriteManifest> = emptyMap()
    private lateinit var progress : ProgressBar

    override fun init(library: CacheLibrary) {
        val files = getFiles(spritesDirectory, "png")
        val alreadyPacked = mutableListOf<String>()
        progress = progress("Packing OSRS Sprites", files.size)

        if (spriteManifest.exists()) {
            val type = object : TypeToken<Map<String, SpriteManifest>>() {}.type
            manifest = Gson().fromJson(spriteManifest.readText(), type)
        } else {
            manifest = emptyMap()
        }

        files.forEach { spriteFile ->
            processSpriteFile(spriteFile, alreadyPacked, library)
        }

        customSprites.forEach { (id, spriteSet) ->
            library.put(SPRITES, id, spriteSet.encode().array())
        }
        progress.close()
    }

    private fun processSpriteFile(spriteFile: File, alreadyPacked: MutableList<String>, library: CacheLibrary) {
        val fileName = spriteFile.nameWithoutExtension
        manifest[fileName]?.let { data ->
            if (data.atlas != null) {
                packFromAtlas(spriteFile, data, library)
            } else {
                packNamedSprite(spriteFile, data)
            }
            alreadyPacked.add(spriteFile.name)
            progress.step()
        } ?: handleUnNamedSprite(spriteFile, library)
    }

    private fun packFromAtlas(spriteFile: File, data: SpriteManifest, library: CacheLibrary) {
        data.atlas?.let { atlas ->
            val sprites = extractSpriteSheet(loadImage(spriteFile), atlas.width, atlas.height)
            sprites.forEachIndexed { index, image ->
                val sprite = Sprite(data.offsetX, data.offsetY, image)
                addSpriteToSet(data.id, sprite, image.width, image.height, index)
            }
        }
    }

    private fun packNamedSprite(spriteFile: File, data: SpriteManifest) {
        val image = loadImage(spriteFile)
        val sprite = Sprite(data.offsetX, data.offsetY, image)
        addSpriteToSet(data.id, sprite, image.width, image.height, 0)
    }

    private fun handleUnNamedSprite(spriteFile: File, library: CacheLibrary) {
        if (spriteFile.name.matches(Regex("^[_0-9]+\\.png$"))) {
            val (group, index) = spriteFile.nameWithoutExtension.split("_").let {
                it[0].toInt() to it.getOrElse(1) { "0" }.toInt()
            }
            var sprites = emptyList<Sprite>().toMutableList()
            if (index != 0) {
                sprites = SpriteSet.decode(group, Unpooled.wrappedBuffer(library.data(SPRITES, group))).sprites
                sprites[index] = Sprite(0, 0, loadImage(spriteFile))
            } else {
                sprites.add(Sprite(0, 0, loadImage(spriteFile)))
            }

            sprites.forEachIndexed { pos, sprite ->
                addSpriteToSet(group, sprite, sprite.width, sprite.height, pos)
            }
            progress.step()
        }
    }

    private fun extractSpriteSheet(sheet : BufferedImage, spriteWidth : Int, spriteHeight : Int): MutableList<BufferedImage> {
        val sprites = emptyList<BufferedImage>().toMutableList()
        val spritesAcross = sheet.width / spriteWidth
        val spritesDown = sheet.height / spriteHeight

        for (y in 0 until spritesDown) {
            for (x in 0 until spritesAcross) {
                val sprite = sheet.getSubimage(x * spriteWidth, y * spriteHeight, spriteWidth, spriteHeight)
                sprites.add(sprite)
            }
        }
        return sprites
    }

    private fun addSpriteToSet(key: Int, sprite: Sprite, width: Int, height: Int, index: Int) {
        customSprites.getOrPut(key) {
            SpriteSet(id = key, width = width, height = height, sprites = mutableListOf())
        }.sprites.add(index, sprite)
    }

    private fun loadImage(path: File): BufferedImage = ImageIO.read(path)

}