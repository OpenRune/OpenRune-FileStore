package dev.openrune.cache.tools.tasks.impl

import dev.openrune.OsrsCacheProvider
import dev.openrune.cache.CLIENTSCRIPT
import dev.openrune.cache.GAMEVALS
import dev.openrune.cache.tools.TaskPriority
import dev.openrune.cache.tools.tasks.CacheTask
import dev.openrune.cache.tools.typeDumper.TypeDumper.Companion.unpackGameVal
import dev.openrune.cache.util.progress
import dev.openrune.clientscript.compiler.ClientScripts
import dev.openrune.definition.Js5GameValGroup
import dev.openrune.definition.type.DBTableType
import dev.openrune.filesystem.Cache
import java.io.File
import java.util.*

class PackCs2(private val cs2Dir: File) : CacheTask() {

    override val priority: TaskPriority get() = TaskPriority.VERY_LAST

    private var valsToUpdate: Map<Int, Int> = emptyMap()
    private val savedValsFile = File(
        System.getProperty("java.io.tmpdir"),
        "openrune-gameval-hashes-${cs2Dir.name}.properties"
    )

    override fun init(cache: Cache) {
        try {
            val configFile = File(cs2Dir, "neptune.toml")
            if (!configFile.exists()) {
                println("Missing neptune cs2 setup.")
                return
            }

            val savedVals = loadSavedVals()
            valsToUpdate = collectValsToUpdate(cache, savedVals)
            dumpCacheVals(File(cs2Dir,"symbols"),cache)

            val scripts = ClientScripts.compileTask(configFile.toPath(), 230)
            val progress = progress("Packing Cs2 Scripts", scripts.size)

            scripts.forEach { script ->
                if (!script.archiveName.contains(script.id.toString())) {
                    cache.write(CLIENTSCRIPT, script.archiveName, script.bytes)
                } else {
                    cache.write(CLIENTSCRIPT, script.id, script.bytes)
                }
                progress.step()
            }

            progress.close()
            saveCurrentVals(cache)
        }catch (e : Exception) {
            e.printStackTrace()
        }
    }

    private fun collectValsToUpdate(cache: Cache, savedVals: Map<Int, Int>): Map<Int, Int> {
        val currentVals = collectCurrentValCrcs(cache)
        val toUpdate = mutableMapOf<Int, Int>()

        currentVals.forEach { (group, crc) ->
            if (savedVals[group] != crc || !savedVals.containsKey(group)) {
                toUpdate[group] = crc
            }
        }

        return toUpdate
    }

    private fun collectCurrentValCrcs(cache: Cache): Map<Int, Int> = mapOf(
        Js5GameValGroup.OBJTYPES.id to cache.crc(GAMEVALS, Js5GameValGroup.OBJTYPES.id),
        Js5GameValGroup.LOCTYPES.id to cache.crc(GAMEVALS, Js5GameValGroup.LOCTYPES.id),
        Js5GameValGroup.NPCTYPES.id to cache.crc(GAMEVALS, Js5GameValGroup.NPCTYPES.id),
        Js5GameValGroup.INVTYPES.id to cache.crc(GAMEVALS, Js5GameValGroup.INVTYPES.id),
        Js5GameValGroup.VARBITTYPES.id to cache.crc(GAMEVALS, Js5GameValGroup.VARBITTYPES.id),
        Js5GameValGroup.SEQTYPES.id to cache.crc(GAMEVALS, Js5GameValGroup.SEQTYPES.id),
        Js5GameValGroup.ROWTYPES.id to cache.crc(GAMEVALS, Js5GameValGroup.ROWTYPES.id),
        Js5GameValGroup.TABLETYPES.id to cache.crc(GAMEVALS, Js5GameValGroup.TABLETYPES.id),
        Js5GameValGroup.IFTYPES.id to cache.crc(GAMEVALS, Js5GameValGroup.IFTYPES.id),
    )

    private fun loadSavedVals(): Map<Int, Int> {
        if (!savedValsFile.exists()) return emptyMap()

        val props = Properties().apply { savedValsFile.inputStream().use { load(it) } }

        return props.entries.associate { (key, value) ->
            key.toString().toInt() to value.toString().toInt()
        }
    }
    private fun saveCurrentVals(cache: Cache) {
        val props = Properties()
        collectCurrentValCrcs(cache).forEach { (group, crc) ->
            props[group.toString()] = crc.toString()
        }

        savedValsFile.outputStream().use { outputStream ->
            props.store(outputStream, "GameVal CRCs")
        }
    }

    private fun dumpCacheVals(basePath : File,cache: Cache) {
        symDumper(basePath,cache,"obj", Js5GameValGroup.OBJTYPES)
        symDumper(basePath,cache,"loc", Js5GameValGroup.LOCTYPES)
        symDumper(basePath,cache,"npc", Js5GameValGroup.NPCTYPES)
        symDumper(basePath,cache,"inv", Js5GameValGroup.INVTYPES)
        symDumper(basePath,cache,"varbit", Js5GameValGroup.VARBITTYPES)
        symDumper(basePath,cache,"seq", Js5GameValGroup.SEQTYPES)
        symDumper(basePath,cache,"dbrow", Js5GameValGroup.ROWTYPES)

        symDumperInterface(basePath,cache)
        symDumperDBTables(basePath,cache)
    }

    private fun symDumper(basePath: File,cache: Cache, name: String, group: Js5GameValGroup) {
        if (!valsToUpdate.containsKey(group.id)) return
        if (group == Js5GameValGroup.TABLETYPES) return

        val builder = StringBuilder()
        cache.files(GAMEVALS, group.id).forEach { file ->
            unpackGameVal(group, file, cache.data(GAMEVALS, group.id, file), builder)
        }

        val transformed = builder.lines()
            .filter { it.isNotBlank() }
            .joinToString("\n") { line ->
                val (n, id) = line.split(":")
                "$id\t$n"
            }


        File(basePath,"$name.sym").writeText("${transformed}\n")
    }

    private fun symDumperDBTables(basePath: File,cache: Cache) {
        if (!valsToUpdate.keys.any { it == Js5GameValGroup.TABLETYPES.id || it == Js5GameValGroup.ROWTYPES.id }) return

        val content = buildString {
            cache.files(GAMEVALS, Js5GameValGroup.TABLETYPES.id).forEach { file ->
                unpackGameVal(Js5GameValGroup.TABLETYPES, file, cache.data(GAMEVALS, Js5GameValGroup.TABLETYPES.id, file), this)
            }
        }

        val dbColumn = StringBuilder()
        val dbTables = mutableSetOf<String>()
        val dbTableTypes = mutableMapOf<Int, DBTableType>()

        OsrsCacheProvider.DBTableDecoder().load(cache, dbTableTypes)

        content.lineSequence().forEach { line ->
            val parts = line.split(".")
            if (parts.size != 4) return@forEach

            val (tableName, colName, tableIdStr, colIdStr) = parts
            val tableId = tableIdStr.toIntOrNull() ?: return@forEach
            val colId = colIdStr.toIntOrNull() ?: return@forEach

            dbTables.add("$tableId\t$tableName")

            val tableType = dbTableTypes[tableId] ?: return@forEach
            val packedColumn = (tableId shl 12) or (colId shl 4)

            tableType.columns[colId]?.types?.map { it.name.lowercase().replace("coordgrid", "coord") }?.let { types ->
                if (types.isNotEmpty()) {
                    if (types.size > 1) {
                        dbColumn.appendLine("$packedColumn\t$tableName:$colName\t${types.joinToString(",")}")
                    }
                    types.forEachIndexed { index, typeName ->
                        val indexedName = if (types.size > 1) "$colName:$index" else colName
                        val indexedId = if (types.size > 1) packedColumn + (index + 1) else packedColumn
                        dbColumn.appendLine("$indexedId\t$tableName:$indexedName\t$typeName")
                    }
                }
            }
        }

        dbColumn.appendLine()

        File(basePath,"dbcolumn.sym").writeText(dbColumn.toString())
        File(basePath,"dbtable.sym").writeText("${dbTables.joinToString("\n")}\n")
    }

    private fun symDumperInterface(basePath: File,cache: Cache) {
        if (!valsToUpdate.containsKey(Js5GameValGroup.IFTYPES.id)) return

        val interfaces = StringBuilder()
        val components = StringBuilder()
        val processedInterfaces = mutableSetOf<String>()
        val processedComponents = mutableSetOf<String>()

        buildString {
            cache.files(GAMEVALS, Js5GameValGroup.IFTYPES.id).forEach { file ->
                unpackGameVal(Js5GameValGroup.IFTYPES, file, cache.data(GAMEVALS, Js5GameValGroup.IFTYPES.id, file), this)
            }
        }.lineSequence().forEach { line ->
            val parts = line.split("-")
            if (parts.size != 4) return@forEach
            val (interfaceName, interfaceId, componentName, componentId) = parts
            val interfaceEntry = "$interfaceId\t$interfaceName"
            if (processedInterfaces.add(interfaceEntry)) {
                interfaces.appendLine(interfaceEntry)
            }

            val componentEntry = "${pack(interfaceId.toInt(), componentId)}\t$interfaceName:$componentName"
            if (processedComponents.add(componentEntry)) {
                components.appendLine(componentEntry)
            }
        }

        interfaces.appendLine()
        components.appendLine()

        File(basePath,"interface.sym").writeText(interfaces.toString())
        File(basePath,"component.sym").writeText(components.toString())
    }

    private fun pack(interfaceId: Int, componentId: String): Int {
        return (interfaceId and 0xFFFF) shl 16 or (componentId.toInt() and 0xFFFF)
    }
}
