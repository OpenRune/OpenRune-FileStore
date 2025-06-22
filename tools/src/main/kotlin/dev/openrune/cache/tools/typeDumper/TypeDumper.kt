package dev.openrune.cache.tools.typeDumper

import com.github.michaelbull.logging.InlineLogger
import dev.openrune.OsrsCacheProvider
import dev.openrune.cache.*
import dev.openrune.cache.gameval.GameValHandler
import dev.openrune.cache.tools.typeDumper.impl.IfTypes
import dev.openrune.cache.tools.typeDumper.impl.TablesAndRows
import dev.openrune.cache.util.Namer
import dev.openrune.definition.GameValGroupTypes
import dev.openrune.definition.GameValGroupTypes.*
import dev.openrune.definition.util.readString
import dev.openrune.filesystem.Cache
import io.netty.buffer.Unpooled
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

enum class Language(val ext: String) {
    KOTLIN(".kt"),
    JAVA(".java"),
    RSCM(".rscm")
}

data class TypeNameOverride(val valType: GameValGroupTypes, val name: String)

data class GameValSettings(
    val usePackedComponents: Boolean = true,
    val combineInterfaceAndComponents: Boolean = false,
    val combineTablesAndRows: Boolean = false,
)

data class TypeExportSettings(
    val packageName: String = "",
    val ignoreNullObjects : Boolean = false,
    val overrideNames: List<TypeNameOverride> = emptyList(),
    var useGameVal: GameValSettings? = null
) {
    companion object {
        fun objects(name: String) = TypeNameOverride(LOCTYPES, name)
        fun npcs(name: String) = TypeNameOverride(NPCTYPES, name)
        fun invs(name: String) = TypeNameOverride(INVTYPES, name)
        fun varps(name: String) = TypeNameOverride(VARPTYPES, name)
        fun varbits(name: String) = TypeNameOverride(VARBITTYPES, name)
        fun items(name: String) = TypeNameOverride(OBJTYPES, name)
        fun sequences(name: String) = TypeNameOverride(SEQTYPES, name)
        fun spotAnimations(name: String) = TypeNameOverride(SPOTTYPES, name)
        fun rows(name: String) = TypeNameOverride(ROWTYPES, name)
        fun tables(name: String) = TypeNameOverride(TABLETYPES, name)
        fun jingles(name: String) = TypeNameOverride(SOUNDTYPES, name)
        fun sprites(name: String) = TypeNameOverride(SPRITETYPES, name)
        fun iftypes(name: String) = TypeNameOverride(IFTYPES, name)
    }
}

class TypeDumper(
    private val cache: Cache,
    private val rev: Int,
    val exportSettings: TypeExportSettings = TypeExportSettings()
) {
    private val logger = InlineLogger()
    private val namer = Namer()
    lateinit var language: Language
    private var names: MutableMap<Int, String> = mutableMapOf()
    private lateinit var outputPath: Path


    init {
        CacheManager.init(OsrsCacheProvider(cache, rev))
    }

    fun dump(language: Language, outputPath: Path = Path.of("./${language.name}")) {
        GameValGroupTypes.entries.forEach { names[it.id] = it.groupName }
        exportSettings.overrideNames.forEach { names[it.valType.id] = it.name }

        if (exportSettings.useGameVal != null && rev < 230) {
            println("Unable to use gameVal for this cache, using lite mode.")
            exportSettings.useGameVal = null
        }

        this.outputPath = outputPath
        this.language = language

        if (!Files.exists(outputPath)) {
            Files.createDirectory(outputPath)
            logger.info { "Creating directory: $outputPath" }
        } else if (!Files.isDirectory(outputPath)) {
            logger.error { "Output path must be a directory!" }
        }

        if (exportSettings.useGameVal != null) {
            writeGameVals()
        } else {
            writeItems()
            writeNpcs()
            writeObjs()
        }
    }

    private fun writeItems() {
        val (builder, path) = generateWriter(LOCTYPES)
        namer.used.clear()
        CacheManager.getItems().filterNot { it.value.isPlaceholder }.forEach { (index, item) ->
            val rawName = if (item.noteTemplateId > 0) "${item.name}_NOTES" else item.name
            if (rawName.isNotBlank()) {
                val name = namer.name(rawName, index.toString())
                write(builder, fieldName(name, index.toString()))
            }
        }
        endWriter(builder, path)
    }

    private fun writeNpcs() {
        val (builder, path) = generateWriter(NPCTYPES)
        namer.used.clear()
        CacheManager.getNpcs().forEach { (index, npc) ->
            val rawName = npc.name.replace("?", "")
            val name = if (rawName.isNotEmpty()) namer.name(npc.name, index.toString()) else "NULL_$index"
            write(builder, fieldName(name, index.toString()))
        }
        endWriter(builder, path)
    }

    private fun writeObjs() {
        val (builder, path) = generateWriter(OBJTYPES)
        namer.used.clear()
        CacheManager.getObjects().forEach { (index, obj) ->
            val rawName = obj.name.replace("?", "")
            if (rawName.isNotEmpty() && rawName != "null") {
                val name = namer.name(obj.name, index.toString())
                write(builder, fieldName(name, index.toString()))
            }
        }
        endWriter(builder, path)
    }

    fun fieldName(name: String?, index: String): String = when (language) {
        Language.KOTLIN -> "const val ${name?.uppercase()} = $index"
        Language.RSCM -> "${name}:${index}"
        else -> "public static final int ${name?.uppercase()} = $index;"
    }

    fun generateWriter(type: GameValGroupTypes = LOCTYPES, override : String = ""): Pair<StringBuilder, Path> {
        val builder = StringBuilder()
        var fileName = if (names.contains(type.id)) names[type.id] ?: type.groupName else type.groupName

        if (override.isNotEmpty()) {
            fileName = override
        }
        val filePath = outputPath.resolve("$fileName${language.ext}")

        if (language != Language.RSCM) {
            builder.appendLine("/* Auto-generated file using ${this::class.java} */")
            builder.appendLine("package ${exportSettings.packageName}")
            builder.appendLine()
            val classDeclaration = if (language == Language.KOTLIN) "object" else "public class"
            builder.appendLine("$classDeclaration ${fileName.removeSuffix(".kt").removeSuffix(".java")} {")
            builder.appendLine()
        }

        return builder to filePath
    }

    fun write(builder: StringBuilder, text: String) {
        if (language == Language.RSCM) builder.appendLine(text.lowercase())
        else builder.appendLine("    $text")
    }

    fun endWriter(builder: StringBuilder, path: Path) {
        if (language != Language.RSCM) {
            builder.appendLine("    /* Auto-generated file using ${this::class.java} */")
            builder.appendLine("}")
        }
        path.toFile().writeText(builder.toString())
    }

    private fun writeGameVals() {

        val writeToJava = language != Language.RSCM

        GameValGroupTypes.entries.forEach { group ->
            namer.used.clear()

            when (group) {
                IFTYPES -> IfTypes(this,writeToJava).writeInterfacesAndComponents(group,cache)
                TABLETYPES -> TablesAndRows(this, writeToJava).writeTablesAndRows(group,cache)
                else -> if (group != ROWTYPES) writeGeneralGroupData(group, writeToJava)
            }
        }
    }

    fun writeGeneralGroupData(group: GameValGroupTypes, writeToJava: Boolean, nameOverride : String = "") {
        val (builder, path) = generateWriter(group,nameOverride)
        val gamevals = GameValHandler.readGameVal(group,cache)
        gamevals.forEach {
            val id = it.id.toString()
            val key = it.name

            val name = namer.name(key, id, language, writeToJava)
            if (exportSettings.ignoreNullObjects && group == LOCTYPES) {
                if (CacheManager.getObject(id.toInt())?.name != "null") {
                    write(builder, fieldName(name, id))
                }
            } else {
                write(builder, fieldName(name, id))
            }
        }

        endWriter(builder, path)
    }

}