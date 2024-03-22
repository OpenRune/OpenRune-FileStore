package dev.openrune.cache.tools

import dev.openrune.cache.CacheManager
import dev.openrune.cache.util.Namer
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.PrintWriter
import java.nio.file.Files
import java.nio.file.Path

enum class Language {
    KOTLIN,
    JAVA
}

class DumpTypeId(
    private val cache: Path,
    private val rev: Int,
    private val outputPath: Path,
    private val packageName: String
) {

    private val logger = KotlinLogging.logger {}

    private val namer = Namer()

    private lateinit var language: Language

    fun init(language: Language = Language.KOTLIN, fileNames: List<String> = listOf("items", "npcs", "objs", "objsNull")) {
        this.language = language
        CacheManager.init(cache, rev)
        if (!Files.exists(outputPath)) {
            Files.createDirectory(outputPath)
            logger.info { "Output path does not exist. Creating directory: $outputPath" }
        } else if (!Files.isDirectory(outputPath)) {
            logger.error { "Output path specified is a file - it must be a directory!" }
        }

        writeItems(fileNames[0])
        writeNpcs(fileNames[1])
        writeObjs(fileNames[2])
        writeObjsNull(fileNames[3])
    }

    private fun writeItems(fileName: String) {
        val file = generateWriter(fileName)
        for ((index, item) in CacheManager.getItems().withIndex()) {
            if (item.isPlaceholder) continue
            val rawName = if (item.noteTemplateId > 0) item.name + "_NOTED" else item.name
            if (rawName.isNotBlank()) {
                val name = namer.name(rawName, index)
                write(file, fieldName(name, index))
            }
        }
        endWriter(file)
    }

    private fun writeNpcs(fileName: String) {
        val file = generateWriter(fileName)
        for ((index, npc) in CacheManager.getNpcs().withIndex()) {
            val rawName = npc.name.replace("?", "")
            val useNullName = rawName.isNotEmpty() && rawName.isNotBlank()
            val name = if (useNullName) namer.name(npc.name, index) else "NULL_$index"
            write(file, fieldName(name, index))
        }
        endWriter(file)
    }

    private fun writeObjs(fileName: String) {
        val file = generateWriter(fileName)
        for ((index, obj) in CacheManager.getObjects().withIndex()) {
            val rawName = obj.name.replace("?", "")
            if (rawName.isNotEmpty() && rawName.isNotBlank()) {
                val useNullName = rawName.isNotEmpty() && rawName.isNotBlank()
                val name = if (useNullName) namer.name(obj.name, index) else "NULL_$index"
                write(file, fieldName(name, index))
            }
        }
        endWriter(file)
    }

    private fun writeObjsNull(fileName: String) {
        val file = generateWriter(fileName)
        for ((index, obj) in CacheManager.getObjects().withIndex()) {
            val rawName = obj.name.replace("?", "")
            if (rawName.isEmpty() && rawName.isBlank()) {
                val useNullName = rawName.isNotEmpty() && rawName.isNotBlank()
                val name = if (useNullName) namer.name(obj.name, index) else "NULL_$index"
                write(file, fieldName(name, index))
            }
        }
        endWriter(file)
    }

    private fun fieldName(name: String?, index: Int): String {
        return if (language == Language.KOTLIN) {
            "const val $name = $index"
        } else {
            "public static final int $name = $index;"
        }
    }

    private fun generateWriter(fileName: String): PrintWriter {
        val writer = PrintWriter(outputPath.resolve("$fileName.${if (language == Language.KOTLIN) "kt" else "java"}").toFile())
        writer.println("/* Auto-generated file using ${this::class.java} */")
        writer.println("package $packageName")
        writer.println()
        val classDeclaration = if (language == Language.KOTLIN) "object" else "public class"
        writer.println("$classDeclaration ${fileName.removeSuffix(".kt").removeSuffix(".java")} {")
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