package dev.openrune.cache.gameval

import dev.openrune.cache.gameval.impl.Interface
import dev.openrune.cache.gameval.impl.Sprite
import dev.openrune.cache.gameval.impl.Table
import dev.openrune.definition.GameValGroupTypes
import java.io.File

enum class Format {
    RSCM_V1,
    RSCM_V2
}

data class TypeNameOverride(val valType: GameValGroupTypes, val name: String)

fun List<GameValElement>.dump(format: Format): String {
    if (isEmpty()) return ""
    return this.joinToString("\n") { dumpElement(it, format) }
}

fun List<GameValElement>.dumpToFile(file: File, format: Format) {
    file.parentFile?.mkdirs()
    file.writeText(dump(format))
}

fun List<GameValElement>.writeFile(outputDir: File, name: String, format: Format) {
    val extension = if (format == Format.RSCM_V1) ".rscm" else ".rscm"
    dumpToFile(File(outputDir, "$name$extension"), format)
}

fun List<GameValElement>.writeFiles(outputDir: File, typeName: String, format: Format) {
    writeFile(outputDir, typeName, format)
}

fun List<GameValElement>.dumpComponents(
    format: Format,
    usePackedComponents: Boolean = true
): Map<String, String> {
    val interfaces = this.filterIsInstance<Interface>()
    if (interfaces.isEmpty()) return emptyMap()

    val interfaceContent = interfaces.joinToString("\n") { dumpElement(it, format) }
    val componentContent = interfaces.flatMap { it.components.map { comp -> 
        dumpItemWithSub(format, it.name, comp.name, if (usePackedComponents) comp.packed else comp.id)
    } }.joinToString("\n")
    
    return mapOf("interfaces" to interfaceContent, "components" to componentContent)
}

fun List<GameValElement>.dumpComponentsCombined(
    format: Format,
    usePackedComponents: Boolean = true
): Map<String, String> {
    val interfaces = this.filterIsInstance<Interface>()
    if (interfaces.isEmpty()) return emptyMap()

    val content = interfaces.flatMap { inter ->
        listOf(dumpElement(inter, format)) + inter.components.map { comp ->
            dumpItemWithSub(format, inter.name, comp.name, if (usePackedComponents) comp.packed else comp.id)
        }
    }.joinToString("\n")
    
    return mapOf("components" to content)
}

fun List<GameValElement>.writeComponentsFiles(outputDir: File, format: Format, usePackedComponents: Boolean = true) {
    val result = dumpComponents(format, usePackedComponents)
    result.forEach { (name, content) ->
        val extension = if (format == Format.RSCM_V1) ".rscm" else ".rscm"
        File(outputDir, "$name$extension").writeText(content)
    }
}

fun List<GameValElement>.writeComponentsCombinedFile(outputDir: File, name: String, format: Format, usePackedComponents: Boolean = true) {
    val result = dumpComponentsCombined(format, usePackedComponents)
    val extension = if (format == Format.RSCM_V1) ".rscm" else ".rscm"
    result["components"]?.let { File(outputDir, "$name$extension").writeText(it) }
}

fun List<GameValElement>.dumpTables(
    format: Format,
    usePackedColumns: Boolean = true
): Map<String, String> {
    val tables = this.filterIsInstance<Table>()
    if (tables.isEmpty()) return emptyMap()

    val tableContent = tables.joinToString("\n") { dumpElement(it, format) }
    val columnContent = tables.flatMap { table ->
        table.columns.map { column ->
            val value = if (usePackedColumns) (table.id shl 16) or column.id else column.id
            dumpItemWithSub(format, table.name, column.name, value)
        }
    }.joinToString("\n")
    
    return mapOf("tables" to tableContent, "columns" to columnContent)
}

fun List<GameValElement>.dumpTablesCombined(
    format: Format,
    usePackedColumns: Boolean = true
): Map<String, String> {
    val tables = this.filterIsInstance<Table>()
    if (tables.isEmpty()) return emptyMap()

    val content = tables.flatMap { table ->
        listOf(dumpElement(table, format)) + table.columns.map { column ->
            val value = if (usePackedColumns) (table.id shl 16) or column.id else column.id
            dumpItemWithSub(format, table.name, column.name, value)
        }
    }.joinToString("\n")
    
    return mapOf("columns" to content)
}

fun List<GameValElement>.writeTablesFiles(outputDir: File, format: Format, usePackedColumns: Boolean = true) {
    val result = dumpTables(format, usePackedColumns)
    result.forEach { (name, content) ->
        val extension = if (format == Format.RSCM_V1) ".rscm" else ".rscm"
        File(outputDir, "$name$extension").writeText(content)
    }
}

fun List<GameValElement>.writeTablesCombinedFile(outputDir: File, name: String, format: Format, usePackedColumns: Boolean = true) {
    val result = dumpTablesCombined(format, usePackedColumns)
    val extension = if (format == Format.RSCM_V1) ".rscm" else ".rscm"
    result["columns"]?.let { File(outputDir, "$name$extension").writeText(it) }
}

private fun dumpElement(element: GameValElement, format: Format): String {
    return when (element) {
        is Sprite -> when (format) {
            Format.RSCM_V1 -> if (element.index != -1) "${element.name}_${element.index}:${element.id}" else "${element.name}:${element.id}"
            Format.RSCM_V2 -> if (element.index != -1) "${element.name}:${element.index}=${element.id}" else "${element.name}=${element.id}"
        }
        else -> when (format) {
            Format.RSCM_V1 -> "${element.name}:${element.id}"
            Format.RSCM_V2 -> "${element.name}=${element.id}"
        }
    }
}

private fun dumpItemWithSub(format: Format, parent: String, sub: String, value: Int): String {
    return when (format) {
        Format.RSCM_V1 -> "${parent}_$sub:$value"
        Format.RSCM_V2 -> "$parent:$sub=$value"
    }
}

class GameValDumpBuilder(
    private val elements: List<GameValElement>,
    private val format: Format,
    private val outputDir: File,
    private val overrideNames: List<TypeNameOverride> = emptyList(),
    private val groupType: GameValGroupTypes
) {
    private var usePacked: Boolean = true
    private var isCombined: Boolean = false
    private var customName: String? = null

    fun packed(value: Boolean = true) = apply { usePacked = value }
    fun combined(value: Boolean = true) = apply { isCombined = value }
    fun name(value: String) = apply { customName = value }

    fun write() {
        val hasInterfaces = elements.any { it is Interface }
        val hasTables = elements.any { it is Table }
        
        when {
            hasInterfaces && hasTables -> {
                error("Mixed types detected. Use separate builders for interfaces and tables.")
            }
            hasInterfaces -> writeInterfaces()
            hasTables -> writeTables()
            else -> writeStandard()
        }
    }
    
    private fun getDefaultName(): String {
        val override = overrideNames.firstOrNull { it.valType == groupType }
        return override?.name ?: when (groupType) {
            GameValGroupTypes.SPRITETYPES -> "sprites"
            GameValGroupTypes.IFTYPES, GameValGroupTypes.IFTYPES_V2 -> "iftypes"
            GameValGroupTypes.TABLETYPES -> "dbtables"
            GameValGroupTypes.ROWTYPES -> "dbrows"
            else -> groupType.groupName
        }
    }
    
    private fun writeStandard() {
        val name = customName ?: getDefaultName()
        elements.writeFile(outputDir, name, format)
    }
    
    private fun writeInterfaces() {
        if (isCombined) {
            // Combined mode: use IFTYPES_V2 name (or override)
            val name = customName ?: getDefaultName()
            elements.writeComponentsCombinedFile(outputDir, name, format, usePacked)
        } else {
            // Unpacked mode: write separate interface and component files
            elements.writeComponentsFiles(outputDir, format, usePacked)
        }
    }
    
    private fun writeTables() {
        if (isCombined) {
            // Combined mode: use TABLETYPES name (or override)
            val name = customName ?: getDefaultName()
            elements.writeTablesCombinedFile(outputDir, name, format, usePacked)
        } else {
            // Unpacked mode: write separate table and column files
            elements.writeTablesFiles(outputDir, format, usePacked)
        }
    }
}

fun List<GameValElement>.dump(format: Format, outputDir: File, groupType: GameValGroupTypes, overrideNames: List<TypeNameOverride> = emptyList()) = 
    GameValDumpBuilder(this, format, outputDir, overrideNames, groupType)
