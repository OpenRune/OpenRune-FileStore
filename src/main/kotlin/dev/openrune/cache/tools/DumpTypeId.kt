package dev.openrune.cache.tools

import dev.openrune.cache.CacheManager
import dev.openrune.cache.util.Namer
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.PrintWriter
import java.nio.file.Files
import java.nio.file.Path

enum class Type {
    ITEM,
    OBJECT,
    NPC
}
class DumpTypeId(
    private val cache: Path,
    private val rev : Int,
    private val type: Type,
    private val outputPath: Path,
    private val fileName: String,
    private val packageName: String
) {

    private val logger = KotlinLogging.logger {}

    private val namer = Namer()
    private val file = generateWriter(fileName)

    fun dump() {
        CacheManager.init(cache,rev)
        if (!Files.exists(outputPath)) {
            Files.createDirectory(outputPath)
            logger.info { "Output path does not exist. Creating directory: $outputPath" }
        } else if (!Files.isDirectory(outputPath)) {
            logger.error { "Output path specified is a file - it must be a directory!" }
        }
        when (type) {
            Type.ITEM -> writeItems()
            Type.NPC -> writeNpcs()
            Type.OBJECT -> writeObjs()
        }
        endWriter(file)
    }

    private fun writeItems() {
        for ((index, item) in CacheManager.getItems().withIndex()) {
            if (item.isPlaceholder) continue
            val rawName = if (item.noteTemplateId > 0) item.name + "_NOTED" else item.name
            if (rawName.isNotBlank()) {
                val name = namer.name(rawName, index)
                write(file, "const val $name = $index")
            }
        }
    }

    private fun writeNpcs() {
        for ((index, npc) in CacheManager.getNpcs().withIndex()) {
            val rawName = npc.name.replace("?", "")
            val useNullName = rawName.isNotEmpty() && rawName.isNotBlank()
            val name = if (useNullName) namer.name(npc.name, index) else "NULL"
            write(file, "const val $name = $index")
        }
    }

    private fun writeObjs() {
        for ((index, obj) in CacheManager.getObjects().withIndex()) {
            val rawName = obj.name.replace("?", "")
            val useNullName = rawName.isNotEmpty() && rawName.isNotBlank()
            val name = if (useNullName) namer.name(obj.name, index) else "NULL"
            write(file, "const val $name = $index")
        }
    }

    private fun generateWriter(fileName: String): PrintWriter {
        val writer = PrintWriter(outputPath.resolve(fileName).toFile())
        writer.println("/* Auto-generated file using ${this::class.java} */")
        writer.println("package $packageName")
        writer.println()
        writer.println("object ${fileName.removeSuffix(".kt")} {")
        writer.println()
        return writer
    }

    private fun write(writer: PrintWriter, text: String) {
        writer.println("    $text")
    }

    private fun endWriter(writer: PrintWriter) {
        writer.println("    /* Auto-generated file using ${this::class.java} */")
        writer.println("}")
        writer.close()
    }
}

fun main() {
    val cache = Path.of("E:\\RSPS\\OpenRune\\OpenRune-Server\\data\\cache\\")
    val rev = 220
    DumpTypeId(cache,rev,Type.ITEM, Path.of("./test."),"Items.kt","gg.rsmod.plugins.api.cfg").dump()
    DumpTypeId(cache,rev,Type.NPC, Path.of("./test."),"Npcs.kt","gg.rsmod.plugins.api.cfg").dump()
    DumpTypeId(cache,rev,Type.OBJECT, Path.of("./test."),"Objs.kt","gg.rsmod.plugins.api.cfg").dump()
}


