package dev.openrune.cache.tools.tasks.impl

import dev.openrune.toml.decode
import dev.openrune.toml.tomlMapper
import dev.openrune.cache.SPRITES
import dev.openrune.cache.tools.tasks.CacheTask
import dev.openrune.cache.tools.tasks.impl.sprites.Sprite
import dev.openrune.cache.tools.tasks.impl.sprites.SpriteManifest
import dev.openrune.cache.tools.tasks.impl.sprites.SpriteSet
import dev.openrune.cache.util.getFiles
import dev.openrune.cache.util.progress
import dev.openrune.definition.constants.ConstantProvider
import dev.openrune.filesystem.Cache
import io.netty.buffer.Unpooled
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.io.path.readText

/**
 * The `PackSprites` class is responsible for packing sprite images into an OSRS game cache.
 * It supports both single sprite files and spritesheets, automatically handling different packing strategies based
 * on the provided manifest file. This class is a part of the cache update process, ensuring that all new or modified
 * sprites are correctly encoded and stored in the game's cache system.
 *
 * @param spritesDirectory The directory where sprite files are stored. This should include all `.png` files that
 *                         need to be processed.
 * @param spriteManifest Optional parameter specifying the manifest file that contains metadata for sprites. If not
 *                       provided, only unnamed sprites will be packed.
 *
 */
class PackSprites(
    private val spritesDirectory: File,
    private val spriteManifest: File = File(spritesDirectory, "manifest.toml")
) : CacheTask() {

    private val mapper = tomlMapper { }

    companion object {
        val customSprites: MutableMap<Int, SpriteSet> = mutableMapOf()
    }

    private var manifest: MutableMap<String, SpriteManifest> = mutableMapOf()

    override fun init(cache: Cache) {
        val files = getFiles(spritesDirectory, "png", "PNG")
        val alreadyPacked = mutableListOf<String>()

        val progress = progress(
            "Packing OSRS Sprites",
            files.filter { it.extension.contains("png", true) }.size
        )

        if (spriteManifest.exists()) {
            loadManifest()
        }

        files.forEach { spriteFile ->
            progress.extraMessage = spriteFile.name
            processSpriteFile(spriteFile, alreadyPacked, cache)
            progress.step()
        }

        customSprites.forEach { (id, spriteSet) ->
            cache.write(SPRITES, id, 0, spriteSet.encode().array())
        }

        progress.close()
    }

    private fun loadManifest() {

        val text = spriteManifest.toPath().readText()

        val replacements = ConstantProvider.mappings.values.flatMap(Map<String, Int>::entries)
            .associate { (k, v) -> k to v.toString() }

        val replaced = Regex("\"([^\"]+)\"").replace(text) { match ->
            val key = match.groupValues[1]
            replacements[key] ?: match.value
        }

        val raw = mapper.decode<Map<String, Any>>(replaced)

        manifest = raw.mapValues { (_, value) ->
            when (value) {
                is Int -> SpriteManifest(id = value)
                is Long -> SpriteManifest(id = value.toInt())
                is Map<*, *> -> decodeManifestMap(value as Map<String, Any>)
                else -> error("Unsupported manifest entry: $value")
            }
        }.toMutableMap()
    }

    private fun decodeManifestMap(map: Map<String, Any>): SpriteManifest {
        val id = (map["id"] as? Number)?.toInt()
            ?: error("Missing sprite id in manifest.")

        val offsetX = (map["offsetX"] as? Number)?.toInt() ?: 0
        val offsetY = (map["offsetY"] as? Number)?.toInt() ?: 0

        val atlasMap = map["atlas"] as? Map<*, *>

        val atlas = atlasMap?.let {
            SpriteManifest.SpriteAtlas(
                width = (it["width"] as Number).toInt(),
                height = (it["height"] as Number).toInt()
            )
        }

        return SpriteManifest(
            id = id,
            offsetX = offsetX,
            offsetY = offsetY,
            atlas = atlas
        )
    }

    private fun processSpriteFile(
        spriteFile: File,
        alreadyPacked: MutableList<String>,
        cache: Cache
    ) {
        val fileName = spriteFile.nameWithoutExtension.lowercase()

        manifest[fileName]?.let { data ->

            if (data.atlas != null) {
                packFromAtlas(spriteFile, data)
            } else {
                packNamedSprite(spriteFile, data)
            }

            alreadyPacked.add(spriteFile.name)

        } ?: handleUnNamedSprite(spriteFile, cache)
    }

    private fun packFromAtlas(
        spriteFile: File,
        data: SpriteManifest
    ) {
        val atlas = data.atlas ?: return

        val sprites = extractSpriteSheet(
            loadImage(spriteFile),
            atlas.width,
            atlas.height
        )

        sprites.forEachIndexed { index, image ->
            val sprite = Sprite(data.offsetX, data.offsetY, image)

            addSpriteToSet(
                data.id,
                sprite,
                image.width,
                image.height,
                index
            )
        }
    }

    private fun packNamedSprite(
        spriteFile: File,
        data: SpriteManifest
    ) {
        val image = loadImage(spriteFile)

        val sprite = Sprite(
            data.offsetX,
            data.offsetY,
            image
        )

        addSpriteToSet(
            data.id,
            sprite,
            image.width,
            image.height,
            0
        )
    }

    private fun handleUnNamedSprite(
        spriteFile: File,
        cache: Cache
    ) {
        if (!spriteFile.name.matches(Regex("^[_0-9]+\\.png$", RegexOption.IGNORE_CASE))) {
            return
        }

        val (group, index) = spriteFile.nameWithoutExtension
            .split("_")
            .let {
                it[0].toInt() to it.getOrElse(1) { "0" }.toInt()
            }

        var sprites = mutableListOf<Sprite>()

        if (index != 0) {

            sprites = SpriteSet.decode(
                group,
                Unpooled.wrappedBuffer(cache.data(SPRITES, group))
            ).sprites

            while (sprites.size <= index) {
                sprites.add(
                    Sprite(
                        0,
                        0,
                        BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
                    )
                )
            }

            sprites[index] = Sprite(
                0,
                0,
                loadImage(spriteFile)
            )

        } else {

            sprites.add(
                Sprite(
                    0,
                    0,
                    loadImage(spriteFile)
                )
            )
        }

        sprites.forEachIndexed { pos, sprite ->
            addSpriteToSet(
                group,
                sprite,
                sprite.width,
                sprite.height,
                pos
            )
        }
    }

    private fun extractSpriteSheet(
        sheet: BufferedImage,
        spriteWidth: Int,
        spriteHeight: Int
    ): MutableList<BufferedImage> {

        val sprites = mutableListOf<BufferedImage>()

        val spritesAcross = sheet.width / spriteWidth
        val spritesDown = sheet.height / spriteHeight

        for (y in 0 until spritesDown) {
            for (x in 0 until spritesAcross) {

                val sprite = sheet.getSubimage(
                    x * spriteWidth,
                    y * spriteHeight,
                    spriteWidth,
                    spriteHeight
                )

                sprites.add(sprite)
            }
        }

        return sprites
    }

    private fun addSpriteToSet(
        key: Int,
        sprite: Sprite,
        width: Int,
        height: Int,
        index: Int
    ) {
        customSprites
            .getOrPut(key) {
                SpriteSet(
                    id = key,
                    width = width,
                    height = height,
                    sprites = mutableListOf()
                )
            }
            .sprites
            .add(index, sprite)
    }

    private fun loadImage(path: File): BufferedImage {
        return ImageIO.read(path)
    }
}