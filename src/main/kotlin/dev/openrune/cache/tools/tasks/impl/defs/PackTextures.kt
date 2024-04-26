package dev.openrune.cache.tools.tasks.impl.defs

import com.displee.cache.CacheLibrary
import com.google.gson.Gson
import dev.openrune.cache.*
import dev.openrune.cache.filestore.buffer.BufferWriter
import dev.openrune.cache.filestore.buffer.Writer
import dev.openrune.cache.filestore.definition.ConfigEncoder
import dev.openrune.cache.filestore.definition.Definition
import dev.openrune.cache.tools.tasks.CacheTask
import dev.openrune.cache.tools.tasks.impl.sprites.SpriteSet
import dev.openrune.cache.util.getFiles
import dev.openrune.cache.util.progress
import io.netty.buffer.Unpooled
import java.awt.image.BufferedImage
import java.io.File

data class TextureDefinition(
    override var id : Int,
    val isTransparent : Boolean = false,
    val fileIds : IntArray = IntArray(0),
    val combineModes : IntArray = IntArray(0),
    val field2440 : IntArray = IntArray(0),
    val colourAdjustments : IntArray = IntArray(0),
    var averageRgb : Int = 0,
    val animationDirection : Int = 0,
    val animationSpeed : Int = 0
) : Definition {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TextureDefinition

        if (id != other.id) return false
        if (isTransparent != other.isTransparent) return false
        if (!fileIds.contentEquals(other.fileIds)) return false
        if (!combineModes.contentEquals(other.combineModes)) return false
        if (!field2440.contentEquals(other.field2440)) return false
        if (!colourAdjustments.contentEquals(other.colourAdjustments)) return false
        if (averageRgb != other.averageRgb) return false
        if (animationDirection != other.animationDirection) return false
        if (animationSpeed != other.animationSpeed) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + isTransparent.hashCode()
        result = 31 * result + fileIds.contentHashCode()
        result = 31 * result + combineModes.contentHashCode()
        result = 31 * result + field2440.contentHashCode()
        result = 31 * result + colourAdjustments.contentHashCode()
        result = 31 * result + averageRgb
        result = 31 * result + animationDirection
        result = 31 * result + animationSpeed
        return result
    }
}

class TextureEncoder: ConfigEncoder<TextureDefinition>() {
    override fun Writer.encode(definition: TextureDefinition) {
        writeShort(definition.averageRgb)
        writeByte(if(definition.isTransparent) 1 else 0)
        val fileCount = definition.fileIds.size
        writeByte(fileCount)
        for(index in 0..<fileCount) {
            writeShort(definition.fileIds[index])
        }
        if (fileCount > 1) {
            definition.combineModes.forEach { combineMode ->
                writeByte(combineMode)
            }

            definition.field2440.forEach { field2440 ->
                writeByte(field2440)
            }

            definition.colourAdjustments.forEach { colourAdjustment ->
                writeInt(colourAdjustment)
            }
        }

        writeByte(definition.animationDirection)
        writeByte(definition.animationSpeed)
    }
}

class PackTextures(private val textureDir : File) : CacheTask() {
    override fun init(library: CacheLibrary) {
        val size = getFiles(textureDir,"json").size
        val progress = progress("Packing Textures", size)
        if (size != 0) {
            getFiles(textureDir,"json").forEach {

                val def: TextureDefinition = Gson().fromJson(it.readText(), TextureDefinition::class.java)

                if (def.fileIds.isNotEmpty()) {
                    val spriteID = def.fileIds.first()
                    val sprite = SpriteSet.decode(spriteID, Unpooled.wrappedBuffer(library.data(SPRITES, spriteID))).sprites.first()
                    def.averageRgb = averageColorForPixels(sprite.image)
                    println("averageRgb: " + def.averageRgb)
                    val encoder = TextureEncoder()
                    val writer = BufferWriter(4096)
                    with(encoder) { writer.encode(def) }

                    library.put(TEXTURES,def.id,writer.toArray())
                    progress.step()
                }
            }

            progress.close()

        }
    }

    fun averageColorForPixels(image: BufferedImage): Int {
        var redTotal = 0
        var greenTotal = 0
        var blueTotal = 0
        var totalPixels = 0

        for (y in 0 until image.height) {
            for (x in 0 until image.width) {
                val pixel = image.getRGB(x, y)
                if (pixel == 0xff00ff) continue  // Skip magenta pixels

                redTotal += (pixel shr 16) and 0xff
                greenTotal += (pixel shr 8) and 0xff
                blueTotal += pixel and 0xff
                totalPixels++
            }
        }

        if (totalPixels == 0) return 0  // Guard against division by zero if all pixels are magenta

        val averageRed = redTotal / totalPixels
        val averageGreen = greenTotal / totalPixels
        val averageBlue = blueTotal / totalPixels

        var averageRGB = (averageRed shl 16) + (averageGreen shl 8) + averageBlue
        if (averageRGB == 0) averageRGB = 1  // Ensure the color is not completely black if average is calculated as 0

        return averageRGB
    }

}