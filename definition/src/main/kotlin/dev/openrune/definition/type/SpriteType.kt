package dev.openrune.definition.type

import dev.openrune.definition.Definition
import dev.openrune.definition.game.IndexedSprite
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import java.io.File
import java.nio.file.Path
import javax.imageio.ImageIO

enum class SpriteSaveMode {
    /**
     * Saves each sprite individually as a separate PNG file.
     */
    SINGLE_SPRITES,

    /**
     * Saves all sprites in a single sprite sheet image (one PNG file containing all sprites).
     */
    SPRITE_SHEET,

    /**
     * Saves each sprite as an individual PNG file but organizes them into directories based on their sprite id.
     * This creates a folder for each sprite id and places individual sprite images inside it.
     */
    SPRITE_SHEET_INDIVIDUAL;
}

data class SpriteType(
    override var id: Int = -1,
    override var inherit: Int = 1,
    override var debugName : String = "",
    var sprites: Array<IndexedSprite> = emptyArray(),

) : Definition {

    /**
     * Saves the sprite(s) to a specified path based on the provided settings.
     *
     * @param path The directory where the sprite(s) will be saved.
     * @param saveAsSheet If true, saves sprites as a sprite sheet. Defaults to true.
     * @param subIndex The index of the sprite to save if saving as a sprite sheet. Defaults to -1.
     *                 If `saveAsSheet` is false, this is ignored.
     * @return The sprite image (BufferedImage).
     */
    fun getSprite(saveAsSheet: Boolean = true, subIndex: Int = -1): BufferedImage {
        return if (saveAsSheet) {
            if (subIndex == -1) {
                val (spriteSheet, _, _) = createSpriteSheet(this)
                spriteSheet
            } else {
                val singleSprite = sprites[subIndex]
                val spriteImage = singleSprite.toBufferedImage()
                spriteImage
            }
        } else {
            val spriteImage = sprites.first().toBufferedImage()
            spriteImage
        }
    }

    companion object {

        /**
         * Dumps all the sprites to the specified location in the chosen format.
         *
         * @param sprites A map of sprite IDs to their corresponding SpriteType objects.
         * @param saveLocation The directory where the sprites will be saved. Defaults to `./sprites/`.
         * @param spriteSaveMode The method of saving sprites: individual PNG files, sprite sheets, or individual files inside directories.
         */
        @JvmStatic
        fun dumpAllSprites(
            sprites: MutableMap<Int, SpriteType>,
            saveLocation: Path = Path.of("./sprites/"),
            spriteSaveMode: SpriteSaveMode = SpriteSaveMode.SINGLE_SPRITES,
            progressCallback: (Int, Int) -> Unit = { total, done -> }
        ) {
            val saveLocationDir = saveLocation.toFile()
            if (!saveLocationDir.exists()) saveLocationDir.mkdirs()

            val totalSprites = sprites.values.sumOf { it.sprites.size }
            var spritesDone = 0

            sprites.forEach { (id, spriteType) ->
                if (spriteType.sprites.isNotEmpty()) {
                    when (spriteSaveMode) {
                        SpriteSaveMode.SINGLE_SPRITES -> saveSingleSprite(id, spriteType, saveLocation)
                        SpriteSaveMode.SPRITE_SHEET -> saveSpriteSheet(id, spriteType, saveLocation)
                        SpriteSaveMode.SPRITE_SHEET_INDIVIDUAL -> saveSpriteSheetIndividual(id, spriteType, saveLocation)
                    }
                    spritesDone += spriteType.sprites.size
                    progressCallback(totalSprites, spritesDone)
                }
            }
        }

        private fun saveSpriteSheetIndividual(id: Int, spriteType: SpriteType, saveLocation: Path) {
            if (spriteType.sprites.size != 1) {
                val saveDir = File(saveLocation.toFile(), "$id/").apply { mkdirs() }
                spriteType.sprites.forEachIndexed { index, sprite ->
                    val savePath = File(saveDir, "${index}.png")
                    val individualSprite = BufferedImage(
                        if (sprite.width > 0) sprite.width else 1,
                        if (sprite.height > 0) sprite.height else 1,
                        BufferedImage.TYPE_INT_ARGB
                    )
                    val graphics: Graphics2D = individualSprite.createGraphics()
                    graphics.drawImage(sprite.toBufferedImage(), sprite.offsetX, sprite.offsetY, null)
                    graphics.dispose()
                    ImageIO.write(individualSprite, "png", savePath)
                }
            } else {
                val savePath = File(saveLocation.toFile(), "${id}.png")
                ImageIO.write(spriteType.sprites.first().toBufferedImage(), "png", savePath)
            }
        }

        private fun saveSingleSprite(id: Int, spriteType: SpriteType, saveLocation: Path) {
            spriteType.sprites.forEachIndexed { index, sprite ->
                val savePath = File(saveLocation.toFile(), "${id}_${index}.png")
                ImageIO.write(sprite.toBufferedImage(), "png", savePath)
            }
        }

        private fun saveSpriteSheet(id: Int, spriteType: SpriteType, saveLocation: Path) {
            val savePath = File(saveLocation.toFile(), "${id}.png")
            val (spriteSheet, _, _) = createSpriteSheet(spriteType)
            ImageIO.write(spriteSheet, "png", savePath)
        }

        private fun createSpriteSheet(spriteType: SpriteType): Triple<BufferedImage, Int, Int> {
            if (spriteType.sprites.size == 1) {
                val firstSprite = spriteType.sprites.first()
                return Triple(firstSprite.toBufferedImage(), firstSprite.width, firstSprite.height)
            }

            val validSprites = spriteType.sprites.filter { it.width > 0 && it.height > 0 }
            val spriteMaxWidth = validSprites.maxOfOrNull { it.width } ?: 1
            val spriteMaxHeight = validSprites.maxOfOrNull { it.height } ?: 1

            val columns = 11.coerceAtMost(spriteType.sprites.size)
            val rows = (spriteType.sprites.size + columns - 1) / columns

            val spriteSheetWidth = columns * spriteMaxWidth
            val spriteSheetHeight = rows * spriteMaxHeight
            val spriteSheet = BufferedImage(spriteSheetWidth, spriteSheetHeight, BufferedImage.TYPE_INT_ARGB)
            val graphics: Graphics2D = spriteSheet.createGraphics()

            spriteType.sprites.forEachIndexed { index, sprite ->
                val row = index / columns
                val col = index % columns
                val x = col * spriteMaxWidth
                val y = row * spriteMaxHeight
                graphics.drawImage(sprite.toBufferedImage(), x + sprite.offsetX, y + sprite.offsetY, null)
            }

            graphics.dispose()
            val croppedSheet = spriteSheet.getSubimage(0, 0, spriteSheetWidth, spriteSheetHeight)

            return Triple(croppedSheet, spriteMaxWidth, spriteMaxHeight)
        }
    }
}