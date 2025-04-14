package dev.openrune.cache.tools

import dev.openrune.OsrsCacheProvider
import dev.openrune.cache.*
import dev.openrune.cache.tools.TypeExportSettings.Companion.items
import dev.openrune.cache.util.Namer
import dev.openrune.cache.util.Namer.Companion.formatForClassName
import dev.openrune.definition.Js5GameValGroup
import dev.openrune.definition.Js5GameValGroup.*
import dev.openrune.definition.util.readString
import dev.openrune.filesystem.Cache
import io.github.oshai.kotlinlogging.KotlinLogging
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.Path

enum class Language(val ext: String) {
    KOTLIN(".kt"),
    JAVA(".java"),
    RSCM(".rscm")
}

data class TypeNameOverride(val valType: Js5GameValGroup, val name: String)

data class GameValSettings(
    val usePackedComponents: Boolean = true,
    val combineInterfaceAndComponents: Boolean = false,
)

data class TypeExportSettings(
    val packageName: String = "",
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
        fun sounds(name: String) = TypeNameOverride(SOUNDTYPES, name)
        fun sprites(name: String) = TypeNameOverride(SPRITETYPES, name)
        fun iftypes(name: String) = TypeNameOverride(IFTYPES, name)
    }
}

class TypeDumper(
    private val cache: Cache,
    private val rev: Int,
    private val exportSettings: TypeExportSettings = TypeExportSettings()
) {
    private val logger = KotlinLogging.logger {}
    private val namer = Namer()
    private lateinit var language: Language
    private var names: MutableMap<Int, String> = mutableMapOf()
    private lateinit var outputPath: Path


    init {
        CacheManager.init(OsrsCacheProvider(cache, rev))
    }

    fun dump(language: Language, outputPath: Path = Path.of("./${language.name}")) {
        Js5GameValGroup.entries.forEach { names[it.id] = it.groupName }
        exportSettings.overrideNames.forEach { names[it.valType.id] = it.name }


        exportSettings.useGameVal = null


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
        CacheManager.getNpcs().forEach { (index, npc) ->
            val rawName = npc.name.replace("?", "")
            val name = if (rawName.isNotEmpty()) namer.name(npc.name, index.toString()) else "NULL_$index"
            write(builder, fieldName(name, index.toString()))
        }
        endWriter(builder, path)
    }

    private fun writeObjs() {
        val (builder, path) = generateWriter(OBJTYPES)
        CacheManager.getObjects().forEach { (index, obj) ->
            val rawName = obj.name.replace("?", "") ?: "null"
            if (rawName.isNotEmpty() && rawName != "null") {
                val name = namer.name(obj.name, index.toString())
                write(builder, fieldName(name, index.toString()))
            }
        }
        endWriter(builder, path)
    }

    private fun fieldName(name: String?, index: String): String = when (language) {
        Language.KOTLIN -> "const val ${name?.uppercase()} = $index"
        Language.RSCM -> "${name}:${index}"
        else -> "public static final int ${name?.uppercase()} = $index;"
    }

    private fun generateWriter(type: Js5GameValGroup = LOCTYPES, override : String = ""): Pair<StringBuilder, Path> {
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

    private fun write(builder: StringBuilder, text: String) {
        if (language == Language.RSCM) builder.appendLine(text.lowercase())
        else builder.appendLine("    $text")
    }

    private fun endWriter(builder: StringBuilder, path: Path) {
        if (language != Language.RSCM) {
            builder.appendLine("    /* Auto-generated file using ${this::class.java} */")
            builder.appendLine("}")
        }
        path.toFile().writeText(builder.toString())
    }

    private fun writeGameVals() {
        val gameValData = mutableMapOf<Int, StringBuilder>()



        for (group in cache.archives(GAMEVALS)) {
            val builder = StringBuilder()
            cache.files(GAMEVALS,group).forEach { file ->
                unpackGameVal(group, file, cache.data(GAMEVALS,group,file), builder)
            }
            gameValData[group] = builder
        }

        val writeToJava = language != Language.RSCM

        Js5GameValGroup.entries.forEach { group ->
            val data = gameValData[group.id]

            when (group) {
                IFTYPES -> writeInterfacesAndComponents(group,data, writeToJava)
                else -> writeGeneralGroupData(group, data, writeToJava)
            }
        }
    }

    private fun writeInterfacesAndComponents(group : Js5GameValGroup,data: StringBuilder?, writeToJava: Boolean) {
        val writtenInterfaces = mutableSetOf<String>()

        if (exportSettings.useGameVal!!.combineInterfaceAndComponents) {
            val (components, pathComponents) = generateWriter(group)

            data?.lines()?.forEach { line ->
                val tokens = line.split("-")
                if (tokens.size == 4) {
                    val (interfaceName, interfaceID, _, _) = tokens
                    if (!writtenInterfaces.contains(interfaceName)) {
                        writeComponentData(interfaceName, interfaceID, components, writeToJava)
                        writtenInterfaces.add(interfaceName)
                    }
                }
            }

            if (language == Language.RSCM) {
                data?.lines()?.forEach { line ->
                    val tokens = line.split("-")
                    if (tokens.size == 4) {
                        val (interfaceName, interfaceID, componentName, componentID) = tokens
                        val finalComponentID = getPackedComponentID(interfaceID, componentID)
                        val componentKey = namer.name("$interfaceName.$componentName", finalComponentID, language, writeToJava)
                        write(components, fieldName(componentKey, finalComponentID))
                    }
                }
            } else {
                writeGroupedComponents(data, components, writeToJava)
            }

            endWriter(components, pathComponents)
        } else {
            val (interfaces, pathInterfaces) = generateWriter(override = "interfaces")
            val (components, pathComponents) = generateWriter(group)

            data?.lines()?.forEach { line ->
                val tokens = line.split("-")
                if (tokens.size == 4) {
                    val (interfaceName, interfaceID, componentName, componentID) = tokens
                    if (!writtenInterfaces.contains(interfaceName)) {
                        writeComponentData(interfaceName, interfaceID, interfaces, writeToJava)
                        writtenInterfaces.add(interfaceName)
                    }

                    val finalComponentID = getPackedComponentID(interfaceID, componentID)
                    val splitter = if (language == Language.RSCM ) "." else "_"
                    val componentKey = namer.name("${interfaceName}${splitter}${componentName}", finalComponentID, language, writeToJava)
                    write(components, fieldName(componentKey, finalComponentID))
                }
            }

            endWriter(interfaces, pathInterfaces)
            endWriter(components, pathComponents)
        }
    }

    private fun writeComponentData(interfaceName: String, interfaceID: String, components: StringBuilder, writeToJava: Boolean) {
        val interfaceKey = namer.name(interfaceName, interfaceID, language, writeToJava)!!
        write(components, fieldName(interfaceKey, interfaceID))
    }

    private fun writeGroupedComponents(data: StringBuilder?, components: StringBuilder, writeToJava: Boolean) {
        val groupedComponents = mutableMapOf<String, MutableList<Pair<String, String>>>()

        data?.lines()?.forEach { line ->
            val tokens = line.split("-")
            if (tokens.size == 4) {
                val (interfaceName, interfaceID, componentName, componentID) = tokens
                groupedComponents.computeIfAbsent("${interfaceName}:${interfaceID}") { mutableListOf() }.add(componentName to componentID)
            }
        }

        components.appendLine()
        groupedComponents.forEach { (interfaceName, componentsList) ->
            val parts = interfaceName.split(":")
            if (parts.size == 2) {
                val classBuilder = StringBuilder()
                val classNameDec = if (language == Language.JAVA) "public static final class" else "object"
                classBuilder.append("$classNameDec ${formatForClassName(parts[0])} {")
                classBuilder.appendLine()

                namer.used.clear()
                componentsList.forEach { (componentName, componentID) ->
                    val finalComponentID = getPackedComponentID(parts[1], componentID)
                    val componentKey = namer.name(componentName, finalComponentID, language, writeToJava)
                    write(classBuilder, "\t${fieldName(componentKey, finalComponentID)}")
                }
                classBuilder.appendLine("\t}")
                write(components, classBuilder.toString())
            }
        }
    }

    private fun writeGeneralGroupData(group: Js5GameValGroup, data: StringBuilder?, writeToJava: Boolean) {
        val (builder, path) = generateWriter(group)

        data?.lines()?.forEach { line ->
            val tokens = line.split(":")
            if (tokens.size == 2) {
                val (key, id) = tokens
                val name = namer.name(key, id, language, writeToJava)
                write(builder, fieldName(name, id))
            }
        }

        endWriter(builder, path)
    }

    private fun getPackedComponentID(interfaceID: String, componentID: String): String {
        return exportSettings.useGameVal!!.usePackedComponents.let {
            if (it) pack(interfaceID.toInt(), componentID) else componentID
        }.toString()
    }

    private fun pack(interfaceId: Int, componentId: String): Int {
        return (interfaceId and 0xFFFF) shl 16 or (componentId.toInt() and 0xFFFF)
    }

    private fun unpackGameVal(type: Int, id: Int, bytes: ByteArray?, builder: StringBuilder) {
        val data = Unpooled.wrappedBuffer(bytes)
        when (Js5GameValGroup.fromId(type)) {
            TABLETYPES -> {
                data.readUnsignedByte()
                val tableName = data.readString()
                var columnId = 0
                while (data.readUnsignedByte().toInt() != 0) {
                    val columnName = data.readString()
                    builder.appendLine("$tableName.$columnName.$id.$columnId")
                    columnId++
                }
            }
            IFTYPES -> {
                var interfaceName = data.readString().takeIf { it.isNotEmpty() } ?: "_"
                var componentId = 0
                while (data.readUnsignedByte().toInt() != 0xFF) {
                    val componentName = data.readString().takeIf { it.isNotEmpty() } ?: "_"
                    builder.appendLine("${interfaceName}-${id}-${componentName}-${componentId}")
                    componentId++
                }
            }
            else -> {
                val arr = ByteArray(data.readableBytes())
                data.readBytes(arr)
                val name = String(arr)
                builder.appendLine("$name:$id")
            }
        }
    }
}