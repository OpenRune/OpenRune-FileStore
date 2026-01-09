package dev.openrune.cache.tools.symbols

import dev.openrune.OsrsCacheProvider
import dev.openrune.cache.*
import dev.openrune.cache.gameval.GameValElement
import dev.openrune.cache.gameval.GameValHandler
import dev.openrune.cache.gameval.GameValHandler.elementAs
import dev.openrune.cache.gameval.GameValHandler.lookup
import dev.openrune.cache.gameval.impl.Interface
import dev.openrune.cache.gameval.impl.Sprite
import dev.openrune.cache.gameval.impl.Table
import dev.openrune.definition.GameValGroupTypes
import dev.openrune.definition.type.*
import dev.openrune.filesystem.Cache
import java.nio.file.Path
import kotlin.io.path.writeText

private val STATS = listOf(
    "attack", "defence", "strength", "hitpoints", "ranged", "prayer",
    "magic", "cooking", "woodcutting", "fletching", "fishing",
    "firemaking", "crafting", "smithing", "mining", "herblore",
    "agility", "thieving", "slayer", "farming", "runecraft",
    "hunter", "construction"
)

private fun <T> collectFrom(
    map: Map<Int, T>,
    selector: (T) -> Int
): Sequence<Int> =
    map.values.asSequence()
        .map(selector)
        .filter { it != -1 }

object SymbolUnpacker {

    val npcTypes = mutableMapOf<Int, NpcType>()
    val objectTypes = mutableMapOf<Int, ObjectType>()
    val mapElements = mutableMapOf<Int, MapElementType>()
    val itemTypes = mutableMapOf<Int, ItemType>()
    val worldMapAreas = mutableMapOf<Int, WorldMapAreaType>()
    val bugTemplates = mutableMapOf<Int, BugTemplateType>()
    val enumTypes = mutableMapOf<Int, EnumType>()
    val structs = mutableMapOf<Int, StructType>()
    val worldEntities = mutableMapOf<Int, WorldEntityType>()
    val stringVectors = mutableMapOf<Int, StringVectorType>()
    val params = mutableMapOf<Int, ParamType>()
    val varClan = mutableMapOf<Int, VarClanType>()
    val varClanSettings = mutableMapOf<Int, VarClanSettingsType>()

    fun unpack(cachePath: Path, rev: Int, path : Path) {
        unpack(Cache.load(cachePath), rev,path)
    }

    fun unpack(cache: Cache, rev: Int, path : Path) {
        OsrsCacheProvider.NPCDecoder(rev).load(cache, npcTypes)
        OsrsCacheProvider.ObjectDecoder(rev).load(cache, objectTypes)
        OsrsCacheProvider.AreaDecoder().load(cache, mapElements)
        OsrsCacheProvider.ItemDecoder().load(cache, itemTypes)
        OsrsCacheProvider.WorldMapAreasDecoder(rev).load(cache, worldMapAreas)
        OsrsCacheProvider.BugTemplateDecoder().load(cache, bugTemplates)
        OsrsCacheProvider.EnumDecoder().load(cache, enumTypes)
        OsrsCacheProvider.StructDecoder().load(cache, structs)
        OsrsCacheProvider.WorldEntityDecoder().load(cache, worldEntities)
        OsrsCacheProvider.StringVectorDecoder().load(cache, stringVectors)
        OsrsCacheProvider.ParamDecoder().load(cache, params)
        OsrsCacheProvider.VarClanDecoder().load(cache, varClan)
        OsrsCacheProvider.VarClanSettingDecoder().load(cache, varClanSettings)

        val sprites = GameValHandler.readGameVal(GameValGroupTypes.SPRITETYPES, cache)
        val fonts = cache.archives(FONTS)

        dumpType(path,"stat", getStats(rev))
        dumpType(path,"locshape", locShapes)
        dumpType(path,"category", collectCategories())
        dumpType(path,"graphic", sprites)
        dumpType(path,"wma", worldMapAreas.map { (id, v) -> GameValElement(v.externalName, id) })
        dumpType(path,"bugtemplate", bugTemplates.keys.map { GameValElement("bugtemplate_$it", it) })
        dumpType(path,"mapelement", mapElements.keys.map { GameValElement("mapelement_$it", it) })

        dumpType(path,"loc", GameValHandler.readGameVal(GameValGroupTypes.LOCTYPES, cache))
        dumpType(path,"npc", GameValHandler.readGameVal(GameValGroupTypes.NPCTYPES, cache))
        dumpType(path,"obj", GameValHandler.readGameVal(GameValGroupTypes.OBJTYPES, cache))
        dumpType(path,"inv", GameValHandler.readGameVal(GameValGroupTypes.INVTYPES, cache))
        dumpType(path,"enum", enumTypes.keys.map { GameValElement("enum_$it", it) })
        dumpType(path,"struct", structs.keys.map { GameValElement("struct_$it", it) })
        dumpType(path,"seq", GameValHandler.readGameVal(GameValGroupTypes.SEQTYPES, cache))
        dumpType(path,"dbtable", GameValHandler.readGameVal(GameValGroupTypes.TABLETYPES, cache))
        dumpType(path,"dbrow", GameValHandler.readGameVal(GameValGroupTypes.ROWTYPES, cache))
        dumpType(path,"stringvector", stringVectors.keys.map { GameValElement("stringvector_$it", it) })
        dumpType(path,"varbit", GameValHandler.readGameVal(GameValGroupTypes.VARBITTYPES, cache))
        dumpType(path,"worldentity", worldEntities.keys.map { GameValElement("worldentity_$it", it) })

        dumpTypeArchive(path,"model", MODELS, cache)
        dumpType(path,"fontmetrics", fonts.map { id ->
            GameValElement(sprites.lookup(id)!!.name, id)
        })

        dumpTypeArchive(path,"synth", SOUNDEFFECTS, cache)
        dumpTypeArchive(path,"midi", MUSIC_TRACKS, cache)
        dumpTypeArchive(path,"jingle", MUSIC_JINGLES, cache)
        dumpType(path,"toplevelinterface", listOf(
            GameValElement("toplevelinterface_80",80),
            GameValElement("toplevelinterface_161",161),
            GameValElement("toplevelinterface_164",164),
            GameValElement("toplevelinterface_165",165),
            GameValElement("toplevelinterface_548",548),
            GameValElement("toplevelinterface_601",601),
        ))

        dumpTypeWithType(path,"param", params.keys.map { GameValElement("param_$it", it) },  params.mapValues { (_, v) -> v.type?.name?.lowercase() ?: "int" })
        //Types from Cs2
        dumpTypeWithType(path,"varp", GameValHandler.readGameVal(GameValGroupTypes.VARPTYPES, cache))
        //Types from Cs2
        dumpTypeWithType(path,"varc", GameValHandler.readGameVal(GameValGroupTypes.VARCS, cache, rev))
        dumpTypeWithType(path,"varclan", varClan.keys.map { GameValElement("varclan${varClan.get(it)!!.type!!.name.lowercase()}_$it", it) },  varClan.mapValues { (_, v) -> v.type?.name?.lowercase() ?: "int" })
        dumpTypeWithType(path,"varclansetting", varClanSettings.keys.map { GameValElement("varclansetting${
            varClanSettings[it]!!.type!!.name.lowercase()
        }_$it", it) },  varClanSettings.mapValues { (_, v) -> v.type?.name?.lowercase() ?: "int" })

        dumpType(path,"dbcolumn", dumpDBCols(cache))
        dumpType(path,"component", dumpComponents(cache))
        dumpType(path,"interface", dumpInterfaces(cache))


        dumpConstant(path, "int", mapOf(
            Int.MIN_VALUE to "min_32bit_int",
            Int.MAX_VALUE to "max_32bit_int"
        ))

        dumpConstant(path, "boolean", mapOf(
            0 to "false",
            1 to "true"
        ))

        dumpConstant(path, "clantype", mapOf(
            0 to "clantype_clan",
            1 to "clantype_gim",
            2 to "clantype_pvpa_group"
        ))

        dumpConstant(path, "chatfilter", mapOf(
            0 to "chatfilter_on",
            1 to "chatfilter_friends",
            2 to "chatfilter_off",
            3 to "chatfilter_hide",
            4 to "chatfilter_autochat"
        ))

        dumpConstant(path, "chattype", mapOf(
            0 to "chattype_gamemessage",
            1 to "chattype_modchat",
            2 to "chattype_publicchat",
            3 to "chattype_privatechat",
            4 to "chattype_engine",
            5 to "chattype_loginlogoutnotification",
            6 to "chattype_privatechatout",
            7 to "chattype_modprivatechat",
            9 to "chattype_friendschat",
            11 to "chattype_friendschatnotification",
            14 to "chattype_broadcast",
            26 to "chattype_snapshotfeedback",
            27 to "chattype_obj_examine",
            28 to "chattype_npc_examine",
            29 to "chattype_loc_examine",
            30 to "chattype_friendnotification",
            31 to "chattype_ignorenotification",
            41 to "chattype_clanchat",
            43 to "chattype_clanmessage",
            44 to "chattype_clanguestchat",
            46 to "chattype_clanguestmessage",
            90 to "chattype_autotyper",
            91 to "chattype_modautotyper",
            99 to "chattype_console",
            101 to "chattype_tradereq",
            102 to "chattype_trade",
            103 to "chattype_chalreq_trade",
            104 to "chattype_chalreq_friendschat",
            105 to "chattype_spam",
            106 to "chattype_playerrelated",
            107 to "chattype_10sectimeout",
            109 to "chattype_clancreationinvitation",
            110 to "chattype_chalreq_clanchat",
            114 to "chattype_dialogue",
            115 to "chattype_mesbox"
        ))

        dumpConstant(path, "clienttype", mapOf(
            1 to "clienttype_desktop",
            2 to "clienttype_android",
            3 to "clienttype_ios",
            4 to "clienttype_enhanced_windows",
            5 to "clienttype_enhanced_mac",
            7 to "clienttype_enhanced_android",
            8 to "clienttype_enhanced_ios",
            10 to "clienttype_enhanced_linux"
        ))

        dumpConstant(path, "platformtype", mapOf(
            0 to "platformtype_default",
            1 to "platformtype_steam",
            2 to "platformtype_android",
            3 to "platformtype_apple",
            5 to "platformtype_jagex"
        ))

        dumpConstant(path, "key", mapOf(
            0 to "0",
            1 to "key_f1",
            2 to "key_f2",
            3 to "key_f3",
            4 to "key_f4",
            5 to "key_f5",
            6 to "key_f6",
            7 to "key_f7",
            8 to "key_f8",
            9 to "key_f9",
            10 to "key_f10",
            11 to "key_f11",
            12 to "key_f12",
            13 to "key_escape",
            16 to "key_1",
            17 to "key_2",
            18 to "key_3",
            19 to "key_4",
            20 to "key_5",
            21 to "key_6",
            22 to "key_7",
            23 to "key_8",
            24 to "key_9",
            25 to "key_0",
            26 to "key_minus",
            27 to "key_equals",
            28 to "key_console",
            32 to "key_q",
            33 to "key_w",
            34 to "key_e",
            35 to "key_r",
            36 to "key_t",
            37 to "key_y",
            38 to "key_u",
            39 to "key_i",
            40 to "key_o",
            41 to "key_p",
            42 to "key_left_bracket",
            43 to "key_right_bracket",
            48 to "key_a",
            49 to "key_s",
            50 to "key_d",
            51 to "key_f",
            52 to "key_g",
            53 to "key_h",
            54 to "key_j",
            55 to "key_k",
            56 to "key_l",
            57 to "key_semicolon",
            58 to "key_apostrophe",
            59 to "key_win_left",
            64 to "key_z",
            65 to "key_x",
            66 to "key_c",
            67 to "key_v",
            68 to "key_b",
            69 to "key_n",
            70 to "key_m",
            71 to "key_comma",
            72 to "key_period",
            73 to "key_slash",
            74 to "key_backslash",
            80 to "key_tab",
            81 to "key_shift_left",
            82 to "key_control_left",
            83 to "key_space",
            84 to "key_return",
            85 to "key_backspace",
            86 to "key_alt_left",
            87 to "key_numpad_add",
            88 to "key_numpad_subtract",
            89 to "key_numpad_multiply",
            90 to "key_numpad_divide",
            91 to "key_clear",
            96 to "key_left",
            97 to "key_right",
            98 to "key_up",
            99 to "key_down",
            100 to "key_insert",
            101 to "key_del",
            102 to "key_home",
            103 to "key_end",
            104 to "key_page_up",
            105 to "key_page_down"
        ))

        dumpConstant(path, "setposh", mapOf(
            0 to "setpos_abs_left",
            1 to "setpos_abs_centre",
            2 to "setpos_abs_right",
            3 to "setpos_rel_left",
            4 to "setpos_rel_centre",
            5 to "setpos_rel_right"
        ))

        dumpConstant(path, "setposv", mapOf(
            0 to "setpos_abs_top",
            1 to "setpos_abs_centre",
            2 to "setpos_abs_bottom",
            3 to "setpos_rel_top",
            4 to "setpos_rel_centre",
            5 to "setpos_rel_bottom"
        ))

        dumpConstant(path, "setsize", mapOf(
            0 to "setsize_abs",
            1 to "setsize_minus",
            2 to "setsize_rel"
        ))

        dumpConstant(path, "settextalignh", mapOf(
            0 to "settextalign_left",
            1 to "settextalign_centre",
            2 to "settextalign_right"
        ))

        dumpConstant(path, "settextalignv", mapOf(
            0 to "settextalign_top",
            1 to "settextalign_centre",
            2 to "settextalign_bottom"
        ))

        dumpConstant(path, "windowmode", mapOf(
            0 to "0",
            1 to "windowmode_small",
            2 to "windowmode_resizable"
        ))

        dumpConstant(path, "gameoption", mapOf(
            1 to "gameoption_remove_roof",
            2 to "gameoption_haptic_on_op",
            3 to "gameoption_haptic_on_drag",
            4 to "gameoption_haptic_on_minimenu_open",
            5 to "gameoption_haptic_on_minimenu_entry_hover",
            6 to "gameoption_minimenu_long_press_time",
            7 to "gameoption_midi_volume",
            8 to "gameoption_wave_volume",
            9 to "gameoption_ambient_volume",
            10 to "gameoption_chat_timestamp_mode",
            11 to "gameoption_camera_sensitivity",
            12 to "gameoption_draw_minimenu_header",
            13 to "gameoption_minimenu_mouse_start_index",
            14 to "gameoption_minimenu_spacing",
            15 to "gameoption_afk_timeout"
        ))

        dumpConstant(path, "deviceoption", mapOf(
            2 to "deviceoption_hide_user_name",
            3 to "deviceoption_mute_title_screen",
            4 to "deviceoption_display_fps",
            5 to "deviceoption_fps_limit",
            6 to "deviceoption_brightness",
            10 to "deviceoption_window_width",
            11 to "deviceoption_window_height",
            12 to "deviceoption_window_topmost",
            14 to "deviceoption_draw_distance",
            15 to "deviceoption_ui_quality",
            16 to "deviceoption_display_build_info",
            17 to "deviceoption_full_screen",
            19 to "deviceoption_master_volume",
            20 to "deviceoption_anti_aliasing_sample_level",
            21 to "deviceoption_plugin_safe_mode",
            22 to "deviceoption_is_sfx_8_bit",
            23 to "deviceoption_afk_timeout",
            24 to "deviceoption_anisotropy_exponent",
            25 to "deviceoption_force_disable_rseven",
            26 to "deviceoption_background_fps_limit",
            27 to "deviceoption_ui_scaling",
            28 to "deviceoption_vsync_mode"
        ))

        dumpConstant(path, "menuentrytype", mapOf(
            0 to "menuentrytype_none",
            1 to "menuentrytype_tile",
            2 to "menuentrytype_npc",
            3 to "menuentrytype_loc",
            4 to "menuentrytype_obj",
            6 to "menuentrytype_player",
            7 to "menuentrytype_component",
            8 to "menuentrytype_worldmapelement"
        ))

        dumpConstant(path, "blendmode", mapOf(
            0 to "blendmode_replace",
            1 to "blendmode_vgrad",
            2 to "blendmode_vgrad_trans"
        ))

        dumpConstant(path, "objowner", mapOf(
            0 to "objowner_none",
            1 to "objowner_self",
            2 to "objowner_other",
            3 to "objowner_group"
        ))

        dumpConstant(path, "rgb", mapOf(
            0xff0000 to "red",
            0x00ff00 to "green",
            0x0000ff to "blue",
            0xffff00 to "yellow",
            0xff00ff to "magenta",
            0x00ffff to "cyan",
            0xffffff to "white",
            0x000000 to "black"
        ))

        dumpConstant(path, "opkind", mapOf(
            0 to "opkind_entityserver",
            1 to "opkind_target",
            2 to "opkind_entity",
            3 to "opkind_component",
            4 to "opkind_walk",
            5 to "opkind_shiftop",
            6 to "opkind_player",
            7 to "opkind_use",
            8 to "opkind_cancel",
            9 to "opkind_examine",
            10 to "opkind_unknown",
            11 to "opkind_obj",
            12 to "opkind_loc",
            13 to "opkind_npc",
            14 to "opkind_worldentity"
        ))

        dumpConstant(path, "opmode", mapOf(
            0 to "opmode_always",
            1 to "opmode_never",
            2 to "opmode_shift",
            3 to "opmode_noshift"
        ))

        dumpConstant(path, "iftype", mapOf(
            0 to "iftype_layer",
            3 to "iftype_rectangle",
            4 to "iftype_text",
            5 to "iftype_graphic",
            6 to "iftype_model",
            9 to "iftype_line",
            10 to "iftype_circle",
            11 to "iftype_crmview",
            12 to "iftype_inputfield",
        ))

    }

    fun dumpConstant(path: Path, name: String, values: Map<Int, String>) {
        val dir = path.resolve("constant")
        dir.toFile().mkdirs()

        val text = values
            .filterValues { it.toIntOrNull() == null }
            .entries
            .joinToString("\n") { (key, value) ->
                val formattedKey =
                    if (name.equals("rgb", ignoreCase = true)) formatColour(key) else key.toString()
                "$value\t$formattedKey"
            }

        dir.resolve("$name.sym").writeText(text)
    }


    fun formatColour(colour: Int): String =
        (colour and 0xFFFFFF).toString()

    fun dumpComponents(cache: Cache): List<GameValElement> =
        buildList {
            GameValHandler.readGameVal(GameValGroupTypes.IFTYPES, cache)
                .mapNotNull { it.elementAs<Interface>() }
                .forEach { inf ->
                    inf.components.forEach {
                        add(GameValElement("${inf.name}:${it.name}", it.packed))
                    }
                }
        }

    fun dumpInterfaces(cache: Cache): List<GameValElement> =
        GameValHandler.readGameVal(GameValGroupTypes.IFTYPES, cache)
            .mapNotNull { it.elementAs<Interface>() }
            .map { GameValElement(it.name, it.id) }

    fun dumpDBCols(cache: Cache): List<GameValElement> {
        val dbTableTypes = mutableMapOf<Int, DBTableType>()
        OsrsCacheProvider.DBTableDecoder().load(cache, dbTableTypes)

        return buildList {
            GameValHandler.readGameVal(GameValGroupTypes.TABLETYPES, cache)
                .mapNotNull { it.elementAs<Table>() }
                .forEach { table ->
                    val tableType = dbTableTypes[table.id] ?: return@forEach
                    val base = table.id shl 12

                    table.columns.forEach { column ->
                        val packed = base or (column.id shl 4)
                        val types = tableType.columns[column.id]?.types
                            ?.map { it.name.lowercase().replace("coordgrid", "coord") }
                            ?: return@forEach

                        types.forEachIndexed { i, type ->
                            val id = if (types.size > 1) packed + (i + 1) else packed
                            val name = if (types.size > 1) "${column.name}:$i" else column.name
                            add(GameValElement("${table.name}:$name\t$type", id))
                        }
                    }
                }
        }
    }

    fun dumpTypeArchive(path : Path,name: String, archive: Int, cache: Cache) {
        val content = cache.archives(archive)
            .asSequence()
            .flatMap { group ->
                cache.files(archive, group).asSequence()
                    .map { "$group\t${name}_$group" }
            }
            .distinct()
            .joinToString("\n")

        path.resolve("$name.sym").writeText("$content\n")
    }

    fun dumpTypeWithType(path: Path, name: String, elements: List<GameValElement>, type: Map<Int, String> = emptyMap()) {
        val content = buildString {
            elements.forEach { element ->
                when (val el = element.elementAs<Sprite>()) {
                    null -> appendLine(format(element.id, element.name, type[element.id]))
                    else -> {
                        val suffix = if (el.index != -1) ",${el.index}" else ""
                        appendLine(format(element.id, element.name + suffix, type[element.id]))
                    }
                }
            }
        }
        path.resolve("$name.sym").writeText(content)
    }

    fun dumpType(path : Path,name: String, elements: List<GameValElement>, type: String = "") {
        val content = buildString {
            elements.forEach {
                when (val el = it.elementAs<Sprite>()) {
                    null -> appendLine(format(it.id, it.name, type))
                    else -> {
                        val suffix = if (el.index != -1) ",${el.index}" else ""
                        appendLine(format(it.id, it.name + suffix, type))
                    }
                }
            }
        }
        path.resolve("$name.sym").writeText(content)
    }

    private fun format(id: Int, name: String, type: String?) =
        if (type == null) "$id\t$name" else "$id\t$name\t$type"

    fun collectCategories(): List<GameValElement> =
        sequenceOf(
            collectFrom(npcTypes) { it.category },
            collectFrom(objectTypes) { it.category },
            collectFrom(mapElements) { it.category },
            collectFrom(itemTypes) { it.category }
        )
            .flatten()
            .distinct()
            .map { GameValElement("category_$it", it) }
            .toList()

    fun getStats(rev: Int): List<GameValElement> =
        STATS.mapIndexed { i, name -> GameValElement(name, i) } +
                if (rev >= 235) listOf(GameValElement("sailing", 23)) else emptyList()

    val locShapes = listOf(
        "1","2","3","4","Q","W","E","R","T",
        "5","8","9","A","S","D","F","G","H",
        "Z","X","C","V","0"
    ).mapIndexed { i, v -> GameValElement(v, i) }
}

fun main() {
    val cache = Cache.load(Path.of("C:\\Users\\chris\\Desktop\\Alter\\data\\cache"))
    SymbolUnpacker.unpack(cache, rev = 235,Path.of("C:\\Users\\chris\\Desktop\\sym"))
}