package dev.openrune.cache.tools.tasks.impl

import dev.openrune.OsrsCacheProvider
import dev.openrune.cache.CLIENTSCRIPT
import dev.openrune.cache.CacheDelegate
import dev.openrune.cache.GAMEVALS
import dev.openrune.cache.gameval.GameValHandler
import dev.openrune.cache.gameval.GameValHandler.elementAs
import dev.openrune.cache.gameval.impl.Interface
import dev.openrune.cache.gameval.impl.Table
import dev.openrune.cache.tools.TaskPriority
import dev.openrune.cache.tools.tasks.CacheTask
import dev.openrune.cache.util.progress
import dev.openrune.clientscript.compiler.ClientScripts
import dev.openrune.definition.GameValGroupTypes
import dev.openrune.definition.GameValGroupTypes.IFTYPES
import dev.openrune.definition.type.DBTableType
import dev.openrune.filesystem.Cache
import java.io.File
import java.util.*

class PackCs2(private val cs2Dir: File, private val rev : Int = 230) : CacheTask() {

    override val priority: TaskPriority get() = TaskPriority.VERY_LAST

    private var valsToUpdate: Map<Int, Int> = emptyMap()
    private val savedValsFile = File(
        System.getProperty("java.io.tmpdir"),
        "openrune-gameval-hashes-${cs2Dir.name}.properties"
    )

    override fun init(cache: Cache) {
        try {
            val library = (cache as CacheDelegate).library
            val configFile = File(cs2Dir, "neptune.toml")
            if (!configFile.exists()) {
                println("Missing neptune cs2 setup.")
                return
            }

            val savedVals = loadSavedVals()
            valsToUpdate = collectValsToUpdate(cache, savedVals)
            dumpCacheVals(File(cs2Dir,"symbols"),cache)

            val scripts = ClientScripts.compileTask(configFile.toPath(), rev)
            val progress = progress("Packing Cs2 Scripts", scripts.size)

            scripts.forEach { script ->
                if (!script.archiveName.contains(script.id.toString())) {
                    if (!library.index(CLIENTSCRIPT).contains(script.id)) {
                        library.put(CLIENTSCRIPT,script.id,script.archiveName, script.bytes)
                    } else {
                        library.put(CLIENTSCRIPT,script.archiveName, script.bytes)
                    }
                } else {
                    library.put(CLIENTSCRIPT, script.id, script.bytes)
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
        GameValGroupTypes.OBJTYPES.id to cache.crc(GAMEVALS, GameValGroupTypes.OBJTYPES.id),
        GameValGroupTypes.LOCTYPES.id to cache.crc(GAMEVALS, GameValGroupTypes.LOCTYPES.id),
        GameValGroupTypes.NPCTYPES.id to cache.crc(GAMEVALS, GameValGroupTypes.NPCTYPES.id),
        GameValGroupTypes.INVTYPES.id to cache.crc(GAMEVALS, GameValGroupTypes.INVTYPES.id),
        GameValGroupTypes.VARBITTYPES.id to cache.crc(GAMEVALS, GameValGroupTypes.VARBITTYPES.id),
        GameValGroupTypes.SEQTYPES.id to cache.crc(GAMEVALS, GameValGroupTypes.SEQTYPES.id),
        GameValGroupTypes.ROWTYPES.id to cache.crc(GAMEVALS, GameValGroupTypes.ROWTYPES.id),
        GameValGroupTypes.TABLETYPES.id to cache.crc(GAMEVALS, GameValGroupTypes.TABLETYPES.id),
        IFTYPES.id to cache.crc(GAMEVALS, IFTYPES.id),
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

    fun dumpCacheVals(basePath : File,cache: Cache) {
        symDumper(basePath,cache,"obj", GameValGroupTypes.OBJTYPES)
        symDumper(basePath,cache,"loc", GameValGroupTypes.LOCTYPES)
        symDumper(basePath,cache,"npc", GameValGroupTypes.NPCTYPES)
        symDumper(basePath,cache,"inv", GameValGroupTypes.INVTYPES)
        symDumper(basePath,cache,"varbit", GameValGroupTypes.VARBITTYPES)
        symDumper(basePath,cache,"seq", GameValGroupTypes.SEQTYPES)
        symDumper(basePath,cache,"dbrow", GameValGroupTypes.ROWTYPES)
        symDumper(basePath,cache,"varc", GameValGroupTypes.VARCS)

        symDumperInterface(basePath,cache)
        symDumperDBTables(basePath,cache)
    }

    private fun symDumper(basePath: File,cache: Cache, name: String, group: GameValGroupTypes) {
        if (!valsToUpdate.containsKey(group.id)) return
        if (group == GameValGroupTypes.TABLETYPES) return

        val transformed = GameValHandler.readGameVal(group, cache).map { "${it.id}\t${it.name}" }
        File(basePath, "$name.sym").writeText(transformed.joinToString("\n") + "\n")
    }

    private fun symDumperDBTables(basePath: File, cache: Cache) {
        if (!valsToUpdate.keys.any { it == GameValGroupTypes.TABLETYPES.id || it == GameValGroupTypes.ROWTYPES.id }) return

        val dbTableTypes = mutableMapOf<Int, DBTableType>()
        OsrsCacheProvider.DBTableDecoder().load(cache, dbTableTypes)

        val tables = GameValHandler.readGameVal(GameValGroupTypes.TABLETYPES, cache)

        val dbTables = tables.map { "${it.id}\t${it.name}" }
        File(basePath, "dbtable.sym").writeText(dbTables.joinToString("\n") + "\n")

        val dbColumns = tables.mapNotNull { it.elementAs<Table>() }.flatMap { table ->
            val tableType = dbTableTypes[table.id] ?: return@flatMap emptyList()
            val tableName = table.name
            val packedBase = table.id shl 12

            buildList {
                table.columns.forEach { column ->
                    val colId = column.id
                    val colName = column.name
                    val packedColumn = packedBase or (colId shl 4)

                    val types = tableType.columns[colId]?.types
                        ?.map { it.name.lowercase().replace("coordgrid", "coord") }
                        ?: return@forEach

                    if (types.isEmpty()) return@forEach

                    if (types.size > 1) {
                        add("$packedColumn\t$tableName:$colName\t${types.joinToString(",")}")
                    }

                    types.forEachIndexed { index, typeName ->
                        val indexedName = if (types.size > 1) "$colName:$index" else colName
                        val indexedId = if (types.size > 1) packedColumn + (index + 1) else packedColumn
                        add("$indexedId\t$tableName:$indexedName\t$typeName")
                    }
                }
            }
        }

        File(basePath, "dbcolumn.sym").writeText(dbColumns.joinToString("\n") + "\n")
    }

    private fun symDumperInterface(basePath: File, cache: Cache) {
        if (!valsToUpdate.containsKey(IFTYPES.id)) return

        val infTypes = GameValHandler.readGameVal(IFTYPES, cache)

        val interfaceTypes = infTypes.map { "${it.id}\t${it.name}" }
        File(basePath, "interface.sym").writeText(interfaceTypes.joinToString("\n") + "\n")

        val componentTypes = infTypes.flatMap { inf ->
            inf.elementAs<Interface>()
                ?.components
                ?.map { comp -> "${comp.packed}\t${inf.name}:${comp.name}" }
                ?: emptyList()
        }

        File(basePath, "component.sym").writeText(componentTypes.joinToString("\n") + "\n")
    }

}