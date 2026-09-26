package dev.openrune.rs2.tasks

import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import dev.openrune.Rs2Cache
import dev.openrune.definition.type.Rs2AchievementType
import dev.openrune.definition.type.Rs2QuestType
import dev.openrune.rs2.AchievementDecoder
import dev.openrune.rs2.OpenRs2CacheArchive
import dev.openrune.rs2.QuestDecoder
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

object CollectRs3GroupIronData {

    data class QuestJson(
        val id: Int,
        @SerializedName("quest_gameval") val questGameval: String?,
        val name: String?,
        val sortName: String?,
        val members: Boolean,
        val difficulty: String,
        val questPoints: Int,
        val startCoords: List<Int>,
        val viaCoord: Int?,
        val questPointsReq: Int,
        val icon: Int?,
        @SerializedName("sprite_gameval") val spriteGameval: String?,
        @SerializedName("quest_achievement") val questAchievement: Int?,
        @SerializedName("quest_age") val questAge: Int?,
        @SerializedName("quest_length") val questLength: Int?,
        @SerializedName("quest_hidden") val questHidden: Int?,
        @SerializedName("quest_icon_big") val questIconBig: Int?,
        @SerializedName("quest_name_first_letter_no_accents") val questNameFirstLetterNoAccents: String?,
        @SerializedName("quest_release_year") val questReleaseYear: Int?,
        @SerializedName("quest_series") val questSeries: Int?,
        @SerializedName("quest_series_number") val questSeriesNumber: Int?,
        @SerializedName("quest_start_location") val questStartLocation: Int?,
        @SerializedName("quest_timeline_category") val questTimelineCategory: Int?,
        @SerializedName("quest_voice_acted") val questVoiceActed: Int?,
        @SerializedName("quest_combat") val questCombat: String?,
        @SerializedName("quest_combat_difficulty") val questCombatDifficulty: Int?,
        @SerializedName("quest_holiday_hub_signposting") val questHolidayHubSignposting: Any?,
        @SerializedName("quest_requires_special") val questRequiresSpecial: Int?,
        @SerializedName("quest_requires_morytania") val questRequiresMorytania: Int?,
        @SerializedName("quest_requires_kudos") val questRequiresKudos: Int?,
        val questReqs: List<QuestReqJson>,
        val statReqs: List<StatReqJson>,
        val varpReqs: List<VarReqJson>,
        val varbitReqs: List<VarReqJson>
    )

    data class QuestReqJson(val id: Int, @SerializedName("quest_gameval") val questGameval: String?, val name: String?)

    data class StatReqJson(val stat: Int, val name: String?, val level: Int)

    data class VarReqJson(val id: Int, val gameval: String?, val min: Int, val max: Int, val message: String)

    private val skillNames = listOf(
        "attack", "defence", "strength", "constitution", "ranged", "prayer", "magic", "cooking", "woodcutting",
        "fletching", "fishing", "firemaking", "crafting", "smithing", "mining", "herblore", "agility", "thieving",
        "slayer", "farming", "runecrafting", "hunter", "construction", "summoning", "dungeoneering", "divination",
        "invention", "archaeology", "necromancy"
    )

    data class AchievementDescJson(val type: Int, val text: String)

    data class AchievementJson(
        val id: Int,
        @SerializedName("achievement_gameval") val achievementGameval: String?,
        val name: String?,
        val descriptions: List<AchievementDescJson>,
        val category: Int,
        val subCategory: Int,
        val sprite: Int?,
        @SerializedName("sprite_gameval") val spriteGameval: String?,
        val runescore: Int,
        val locked: Boolean,
        val hide: Int,
        val members: Boolean,
        val prereqItemsToComplete: List<Int>,
        val numPrereqGroupsToComplete: Int,
        val reqGroupItemsToComplete: List<Int>,
        val numReqGroupsToComplete: Int
    )

    private class GamevalFile(val revision: Int, val entries: Map<Int, String>)

    private val gson = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()

    private fun loadGamevals(name: String): Map<Int, String> {
        val stream = CollectRs3GroupIronData::class.java.getResourceAsStream("/gamevals/$name.json")
            ?: error("Missing gameval resource /gamevals/$name.json")
        return stream.bufferedReader().use { gson.fromJson(it, GamevalFile::class.java).entries }
    }

    private val questGamevals by lazy { loadGamevals("quest") }
    private val graphicGamevals by lazy { loadGamevals("graphic") }
    private val achievementGamevals by lazy { loadGamevals("achievement") }
    private val varpGamevals by lazy { loadGamevals("var_player") }
    private val varbitGamevals by lazy { loadGamevals("varbit") }
    private val paramIdsByName by lazy { loadGamevals("param").entries.associate { (id, name) -> name to id } }

    private fun Rs2QuestType.param(name: String): Any? {
        val id = paramIdsByName[name] ?: error("Unknown param gameval $name")
        return params?.get(id)
    }

    private fun Rs2QuestType.intParam(name: String): Int? = param(name) as? Int

    private fun Rs2QuestType.stringParam(name: String): String? = param(name) as? String

    @JvmStatic
    fun main(args: Array<String>) {
        val options = args.toList().windowed(2, 2, partialWindows = true).associate { it[0] to it.getOrNull(1) }
        val out = Paths.get(options["--out"] ?: "build/rs3-data")

        val entry = OpenRs2CacheArchive.allBuilds().last()
        Rs2Cache.loadRemote(id = entry.id, scope = entry.scope).use {
            println("Using build ${it.build}")
            Files.createDirectories(out)
            dumpQuests(it, out.resolve("quests.json"))
            dumpAchievements(it, out.resolve("achievements.json"))
        }
    }

    private fun dumpAchievements(cache: Rs2Cache, file: Path) {
        val achievements = mutableMapOf<Int, Rs2AchievementType>()
        AchievementDecoder().load(cache, achievements)

        val json = achievements.values
            .filter { it.name != null }
            .sortedBy { it.id }
            .map {
                AchievementJson(
                    id = it.id,
                    achievementGameval = achievementGamevals[it.id],
                    name = it.name,
                    descriptions = it.descriptions.map { desc -> AchievementDescJson(desc.type, desc.text) },
                    category = it.category,
                    subCategory = it.subCategory,
                    sprite = it.sprite,
                    spriteGameval = it.sprite?.let { sprite -> graphicGamevals[sprite] },
                    runescore = it.runescore,
                    locked = it.locked,
                    hide = it.hide,
                    members = it.members,
                    prereqItemsToComplete = it.prereqItemsToComplete,
                    numPrereqGroupsToComplete = it.numPrereqGroupsToComplete,
                    reqGroupItemsToComplete = it.reqGroupItemsToComplete,
                    numReqGroupsToComplete = it.numReqGroupsToComplete
                )
            }

        Files.writeString(file, gson.toJson(json))
        println("Wrote ${json.size} achievements to ${file.toAbsolutePath()}")
    }

    private fun dumpQuests(cache: Rs2Cache, file: Path) {
        val quests = mutableMapOf<Int, Rs2QuestType>()
        QuestDecoder().load(cache, quests)

        val json = quests.values
            .filter { it.name != null }
            .sortedBy { it.id }
            .map {
                QuestJson(
                    id = it.id,
                    questGameval = questGamevals[it.id],
                    name = it.name,
                    sortName = it.sortName,
                    members = it.members,
                    difficulty = it.difficulty.name.lowercase(),
                    questPoints = it.questPoints,
                    startCoords = it.startCoords,
                    viaCoord = it.viaCoord,
                    questPointsReq = it.questPointsReq,
                    icon = it.icon,
                    spriteGameval = it.icon?.let { icon -> graphicGamevals[icon] },
                    questAchievement = it.intParam("quest_achievement"),
                    questAge = it.intParam("quest_age"),
                    questLength = it.intParam("quest_length"),
                    questHidden = it.intParam("quest_hidden"),
                    questIconBig = it.intParam("quest_icon_big"),
                    questNameFirstLetterNoAccents = it.stringParam("quest_name_first_letter_no_accents"),
                    questReleaseYear = it.intParam("quest_release_year"),
                    questSeries = it.intParam("quest_series"),
                    questSeriesNumber = it.intParam("quest_series_number"),
                    questStartLocation = it.intParam("quest_start_location"),
                    questTimelineCategory = it.intParam("quest_timeline_category"),
                    questVoiceActed = it.intParam("quest_voice_acted"),
                    questCombat = it.stringParam("quest_combat"),
                    questCombatDifficulty = it.intParam("quest_combat_difficulty"),
                    questHolidayHubSignposting = it.param("quest_holiday_hub_signposting"),
                    questRequiresSpecial = it.intParam("quest_requires_special"),
                    questRequiresMorytania = it.intParam("quest_requires_morytania"),
                    questRequiresKudos = it.intParam("quest_requires_kudos"),
                    questReqs = it.questReqs.map { req -> QuestReqJson(req, questGamevals[req], quests[req]?.name) },
                    statReqs = it.statReqs.map { req -> StatReqJson(req.stat, skillNames.getOrNull(req.stat), req.level) },
                    varpReqs = it.varpReqs.map { req -> VarReqJson(req.variable, varpGamevals[req.variable], req.min, req.max, req.message) },
                    varbitReqs = it.varbitReqs.map { req -> VarReqJson(req.variable, varbitGamevals[req.variable], req.min, req.max, req.message) }
                )
            }

        Files.writeString(file, gson.toJson(json))
        println("Wrote ${json.size} quests to ${file.toAbsolutePath()}")
    }
}
