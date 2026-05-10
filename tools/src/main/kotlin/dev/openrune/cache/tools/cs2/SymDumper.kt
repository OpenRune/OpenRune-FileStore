package dev.openrune.cache.tools.cs2

import dev.openrune.OsrsCacheProvider
import dev.openrune.cache.BUGTEMPLATE
import dev.openrune.cache.CATEGORY
import dev.openrune.cache.CONFIGS
import dev.openrune.cache.MODELS
import dev.openrune.cache.SOUNDEFFECTS
import dev.openrune.cache.STRINGVECTOR
import dev.openrune.cache.STRUCT
import dev.openrune.cache.VARBIT
import dev.openrune.cache.WORLD_ENTITY
import dev.openrune.cache.filestore.definition.FontDecoder
import dev.openrune.cache.gameval.GameValElement
import dev.openrune.cache.gameval.GameValHandler
import dev.openrune.cache.gameval.GameValHandler.elementAs
import dev.openrune.cache.gameval.GameValHandler.lookup
import dev.openrune.cache.gameval.GameValHandler.lookupAs
import dev.openrune.cache.gameval.impl.Interface
import dev.openrune.cache.gameval.impl.Sprite as GameValSprite
import dev.openrune.cache.gameval.impl.Table
import dev.openrune.definition.GameValGroupTypes
import dev.openrune.definition.GameValGroupTypes.IFTYPES
import dev.openrune.definition.type.DBTableType
import dev.openrune.definition.type.EnumType
import dev.openrune.definition.type.ItemType
import dev.openrune.definition.type.MapElementType
import dev.openrune.definition.type.ObjectType
import dev.openrune.definition.type.ParamType
import dev.openrune.definition.type.VarClanSettingsType
import dev.openrune.definition.type.VarClanType
import dev.openrune.definition.type.VarClientType
import dev.openrune.definition.type.VarpType
import dev.openrune.definition.type.WorldMapAreaType
import dev.openrune.filesystem.Cache
import java.io.File
import java.io.FileWriter

/**
 * Writes Neptune / compiler `.sym` files from cache gamevals and related indices (used by [PackCs2]).
 */
class SymDumper {

    fun dumpAll(basePath: File, cache: Cache, rev: Int) {
        dumpSimpleGameValSyms(basePath, cache, rev)
        dumpVarbits(basePath, cache, rev)
        dumpArchiveIndexSyms(basePath, cache)
        dumpInterfaces(basePath, cache, rev)
        dumpDbTables(basePath, cache)
        dumpSprites(basePath, cache, rev)
        dumpEnumSyms(basePath, cache)
        dumpVarClan(basePath, cache)
        dumpVarClanSettings(basePath, cache)
        dumpParam(basePath, cache, rev)
        dumpWma(basePath, cache, rev)
        dumpVarcAndVarp(basePath, cache, rev)
        dumpCategories(basePath, cache, rev)
    }

    private fun writeSymLines(basePath: File, fileName: String, lines: List<String>) {
        File(basePath, fileName).writeText(lines.joinToString("\n") + "\n")
    }

    private fun dumpSimpleGameValSyms(basePath: File, cache: Cache, rev: Int) {
        symDumper(basePath, cache, "obj", GameValGroupTypes.OBJTYPES, rev)
        symDumper(basePath, cache, "loc", GameValGroupTypes.LOCTYPES, rev)
        symDumper(basePath, cache, "npc", GameValGroupTypes.NPCTYPES, rev)
        symDumper(basePath, cache, "inv", GameValGroupTypes.INVTYPES, rev)
        symDumper(basePath, cache, "seq", GameValGroupTypes.SEQTYPES, rev)
        symDumper(basePath, cache, "dbrow", GameValGroupTypes.ROWTYPES, rev)
        symDumper(basePath, cache, "jingle", GameValGroupTypes.SOUNDTYPES, rev)
    }

    private fun dumpArchiveIndexSyms(basePath: File, cache: Cache) {
        symDumperArchiveOnly(basePath, "model", cache, MODELS)
        symDumperArchiveOnly(basePath, "worldentity", cache, CONFIGS, WORLD_ENTITY)
        symDumperArchiveOnly(basePath, "category", cache, CONFIGS, CATEGORY)
        symDumperArchiveOnly(basePath, "bugtemplate", cache, CONFIGS, BUGTEMPLATE)
        symDumperArchiveOnly(basePath, "stringvector", cache, CONFIGS, STRINGVECTOR)
        symDumperArchiveOnly(basePath, "struct", cache, CONFIGS, STRUCT)
        symDumperArchiveOnly(basePath, "synth", cache, SOUNDEFFECTS)
    }

    private fun dumpVarClan(basePath: File, cache: Cache) {
        val varclan = mutableMapOf<Int, VarClanType>()
        OsrsCacheProvider.VarClanDecoder().load(cache, varclan)

        val lines = varclan.map { (id, type) ->
            "$id\tvarclan_${id}\t${type.type?.name?.lowercase()?.replace("coordgrid", "coord")}"
        }
        writeSymLines(basePath, "varclan.sym", lines)
    }


    private fun dumpVarcAndVarp(basePath: File, cache: Cache, rev: Int) {
        val varClients = mutableMapOf<Int, VarClientType>()
        OsrsCacheProvider.VarClientDecoder().load(cache, varClients)

        val varp = mutableMapOf<Int, VarpType>()
        OsrsCacheProvider.VarDecoder().load(cache, varp)

        val varpTypes = GameValHandler.readGameVal(GameValGroupTypes.VARPTYPES, cache, rev)

        val varclanFile = File(basePath, "varclan.sym")
        val varClientEntries = varClients.mapValues { (id, _) -> "varclan_$id" }
        appendMissing(varclanFile, varClientEntries)

        val varpFile = File(basePath, "varp.sym")
        val varpEntries = varp.mapValues { (id, _) ->
            val name = varpTypes.lookup(id)?.name ?: "varplayer"
            "${name}_$id"
        }
        appendMissing(varpFile, varpEntries)
    }

    private fun appendMissing(file: File, newEntries: Map<Int, String>) {
        val existingIds = readExistingIds(file)

        file.parentFile?.mkdirs()

        FileWriter(file, true).use { out ->
            newEntries.forEach { (id, name) ->
                if (id !in existingIds) {
                    out.appendLine("$id\t$name")
                }
            }
        }
    }

    private fun readExistingIds(file: File): MutableSet<Int> {
        if (!file.exists()) return mutableSetOf()

        return file.readLines()
            .mapNotNull { line ->
                line.split("\t").firstOrNull()?.toIntOrNull()
            }
            .toMutableSet()
    }

    private fun dumpParam(basePath: File, cache: Cache, rev: Int) {
        val paramTypes = mutableMapOf<Int, ParamType>()
        OsrsCacheProvider.ParamDecoder(rev).load(cache, paramTypes)

        val lines = paramTypes.map { (id, type) ->
            val typeName = type.type?.name?.lowercase()?.replace("coordgrid", "coord") ?: "int"
            "$id\tparam_$id\t$typeName"
        }
        writeSymLines(basePath, "param.sym", lines)
    }

    private fun dumpVarClanSettings(basePath: File, cache: Cache) {
        val varclanSettings = mutableMapOf<Int, VarClanSettingsType>()
        OsrsCacheProvider.VarClanSettingDecoder().load(cache, varclanSettings)

        val lines = varclanSettings.map { (id, type) ->
            "$id\tvarclansetting_${id}\t${type.type?.name?.lowercase()?.replace("coordgrid", "coord")}"
        }
        writeSymLines(basePath, "varclansetting.sym", lines)
    }

    private fun dumpWma(basePath: File, cache: Cache, rev: Int) {
        val worldMapAreaType = mutableMapOf<Int, WorldMapAreaType>()
        OsrsCacheProvider.WorldMapAreasDecoder(rev).load(cache, worldMapAreaType)

        val lines = worldMapAreaType.map { (id, type) ->
            "$id\t${type.internalName}"
        }

        writeSymLines(basePath, "wma.sym", lines)
    }

    private fun dumpSprites(basePath: File, cache: Cache, rev: Int) {
        val fonts = FontDecoder(cache).loadAllFonts()
        val spritesGamevals = GameValHandler.readGameVal(GameValGroupTypes.SPRITETYPES, cache, rev)
        val lines = fonts.map { (id, _) ->
            val name = spritesGamevals.lookupAs<GameValSprite>(id)?.name ?: error("missing sprite gameval for font id $id")
            "$id\t$name"
        }

        val sprites = StringBuilder()
        spritesGamevals.forEach {
            val gameval = it.elementAs<GameValSprite>() ?: return@forEach
            sprites.appendLine("${gameval.id}\t${gameval.name}${if (gameval.index == -1) "" else ",${gameval.index}" }")
        }

        File(basePath, "graphic.sym").writeText(sprites.toString())
        writeSymLines(basePath, "fontmetrics.sym", lines)
    }

    private fun dumpCategories(basePath: File, cache: Cache, rev: Int) {
        val items = mutableMapOf<Int, ItemType>()
        OsrsCacheProvider.ItemDecoder(rev).load(cache, items)

        val objects = mutableMapOf<Int, ObjectType>()
        OsrsCacheProvider.ObjectDecoder(rev).load(cache, objects)

        val mapel = mutableMapOf<Int, MapElementType>()
        OsrsCacheProvider.AreaDecoder().load(cache, mapel)

        val categories = buildSet {
            items.values.forEach { add(it.category) }
            objects.values.forEach { add(it.category) }
            mapel.values.forEach { add(it.category) }
        }.filter { it != -1 }.toSet()

        val filled = (categories.minOrNull()!!..categories.maxOrNull()!!).toList()

        val catLines = filled.map {
            "${it}\tcategory_${it}"
        }

        val mapelLines = mapel.map { (id, _) ->
            "${id}\tmapelement_${id}"
        }


        writeSymLines(basePath, "mapelement.sym", mapelLines)
        writeSymLines(basePath, "category.sym", catLines)
    }

    private fun dumpEnumSyms(basePath: File, cache: Cache) {
        val enums = mutableMapOf<Int, EnumType>()
        OsrsCacheProvider.EnumDecoder().load(cache, enums)

        val enumLines = enums.map { (id, _) -> "$id\tenum_$id" }
        writeSymLines(basePath, "enum.sym", enumLines)

        val statLines = buildList {
            val statEnum = enums[STAT_ENUM_ID] ?: return@buildList
            statEnum.values.values.forEachIndexed { index, v ->
                add("$index\t${v.toString().lowercase()}")
            }
        }
        writeSymLines(basePath, "stat.sym", statLines)
    }

    private fun symDumperArchiveOnly(
        basePath: File,
        name: String,
        cache: Cache,
        index: Int,
        archiveId: Int = -1,
    ) {
        val lines = if (archiveId == -1) {
            cache.archives(index).map { arch -> "$arch\t${name}_$arch" }
        } else {
            cache.files(index, archiveId).map { fileId -> "$fileId\t${name}_$fileId" }
        }
        writeSymLines(basePath, "$name.sym", lines)
    }

    private fun dumpVarbits(basePath: File, cache: Cache, rev: Int) {
        val varbits = cache.files(CONFIGS, VARBIT)

        val gamevals = GameValHandler.readGameVal(GameValGroupTypes.VARBITTYPES, cache, rev)

        val lines = varbits.map { id ->
            gamevals.lookup(id)?.let { gameval ->
                "${gameval.id}\t${gameval.name}"
            } ?: "$id\tvarplayerbit_$id"
        }


        writeSymLines(basePath, "varbit.sym", lines)
    }

    private fun symDumper(basePath: File, cache: Cache, name: String, group: GameValGroupTypes, rev: Int) {
        val lines = GameValHandler.readGameVal(group, cache, rev).map { "${it.id}\t${it.name}" }
        writeSymLines(basePath, "$name.sym", lines)
    }

    private fun dumpDbTables(basePath: File, cache: Cache) {
        val dbTableTypes = mutableMapOf<Int, DBTableType>()
        OsrsCacheProvider.DBTableDecoder().load(cache, dbTableTypes)

        val tables = GameValHandler.readGameVal(GameValGroupTypes.TABLETYPES, cache)

        writeSymLines(
            basePath,
            "dbtable.sym",
            tables.map { "${it.id}\t${it.name}" },
        )

        val dbColumns = tables
            .mapNotNull { it.elementAs<Table>() }
            .flatMap { table ->
                val tableType = dbTableTypes[table.id] ?: return@flatMap emptyList()
                val tableName = table.name
                buildList {
                    table.columns.forEach { column ->
                        val colId = column.id
                        val colName = column.name
                        val types = tableType.columns[colId]?.types
                            ?.map { it.name.lowercase().replace("coordgrid", "coord") }
                            ?: return@forEach
                        if (types.isEmpty()) return@forEach
                        add("${table.id}:$colId\t$tableName:$colName\t${types.joinToString(",")}")
                        types.forEachIndexed { index, typeName ->
                            add("${table.id}:$colId:$index\t$tableName:$colName:$index\t$typeName")
                        }
                    }
                }
            }

        writeSymLines(basePath, "dbcolumn.sym", dbColumns)
    }

    private fun dumpInterfaces(basePath: File, cache: Cache, rev: Int) {
        val ifTypes: List<GameValElement> =
            GameValHandler.readGameVal(IFTYPES, cache, rev).sortedBy { it.id }

        val mainInterfaces = ifTypes.filter { !it.name.startsWith("toplevel") && it.name !in OVERLAY_INTERFACE_NAMES }
        writeSymLines(basePath, "interface.sym", mainInterfaces.map { "${it.id}\t${it.name}" })

        writeSymLines(
            basePath,
            "toplevelinterface.sym",
            ifTypes.filter { it.name.startsWith("toplevel") }.map { "${it.id}\t${it.name}" },
        )

        writeSymLines(
            basePath,
            "overlayinterface.sym",
            ifTypes.filter { it.name in OVERLAY_INTERFACE_NAMES }.map { "${it.id}\t${it.name}" },
        )

        val componentLines = ifTypes.flatMap { inf ->
            inf.elementAs<Interface>()?.components?.sortedBy { it.id }?.map { comp ->
                "${inf.id}:${comp.id}\t${inf.name}:${comp.name}"
            } ?: emptyList()
        }
        writeSymLines(basePath, "component.sym", componentLines)
    }

    companion object {

        fun dumpCacheVals(basePath: File, cache: Cache, rev: Int) {
            SymDumper().dumpAll(basePath, cache, rev)
        }

        private val OVERLAY_INTERFACE_NAMES = setOf(
            "settings",
            "chatbox",
            "worldmap",
            "collection",
            "reportabuse",
        )

        private const val STAT_ENUM_ID = 680
    }
}
