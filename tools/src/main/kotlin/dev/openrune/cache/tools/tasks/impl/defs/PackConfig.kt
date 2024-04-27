package dev.openrune.cache.tools.tasks.impl.defs

import com.displee.cache.CacheLibrary
import com.google.gson.Gson
import dev.openrune.cache.*
import dev.openrune.cache.filestore.buffer.BufferWriter
import dev.openrune.cache.tools.tasks.CacheTask
import dev.openrune.cache.tools.tasks.impl.defs.json.PackItems
import dev.openrune.cache.tools.tasks.impl.sprites.SpriteSet
import dev.openrune.cache.util.getFiles
import dev.openrune.cache.util.progress
import io.netty.buffer.Unpooled
import java.awt.image.BufferedImage
import java.io.File

enum class ConfigType{
    TEXTURES,
    NPCS
}

class PackConfig(val type : ConfigType,private val directory : File) : CacheTask() {
    override fun init(library: CacheLibrary) {
        val size = getFiles(directory, "json").size
        val progress = progress("Packing Textures", size)
        if (size != 0) {
            getFiles(directory, "json").forEach {

                progress.close()
            }
        }
    }

}

fun main() {
    PackConfig(ConfigType.TEXTURES,File(""))
    PackConfig(ConfigType.NPCS,File(""))

    PackItems(File(""))
}