package dev.openrune.definition.type

import dev.openrune.definition.Definition
import dev.openrune.definition.MutableParameterized

enum class Rs2QuestKind { NORMAL, SEASONAL, TUTORIAL }

enum class Rs2QuestDifficulty(val value: Int) {
    NOVICE(0), INTERMEDIATE(1), EXPERIENCED(2), MASTER(3), GRANDMASTER(4), MULTI(250);

    companion object {
        fun fromValue(value: Int): Rs2QuestDifficulty =
            entries.firstOrNull { it.value == value } ?: error("Invalid quest difficulty $value")
    }
}

data class Rs2QuestVarRange(val variable: Int, val min: Int, val max: Int)

data class Rs2QuestStatReq(val stat: Int, val level: Int)

data class Rs2QuestVarReq(val variable: Int, val min: Int, val max: Int, val message: String)

data class Rs2QuestType(
    override var id: Int = -1,
    var name: String? = null,
    var sortName: String? = null,
    var masterQuestVarps: List<Rs2QuestVarRange> = emptyList(),
    var masterQuestVarbits: List<Rs2QuestVarRange> = emptyList(),
    var wideVarbits: Boolean = false,
    var parent: Int? = null,
    var kind: Rs2QuestKind = Rs2QuestKind.NORMAL,
    var difficulty: Rs2QuestDifficulty = Rs2QuestDifficulty.NOVICE,
    var members: Boolean = false,
    var questPoints: Int = 0,
    var startCoords: List<Int> = emptyList(),
    var viaCoord: Int? = null,
    var questReqs: List<Int> = emptyList(),
    var statReqs: List<Rs2QuestStatReq> = emptyList(),
    var questPointsReq: Int = 0,
    var icon: Int? = null,
    var varpReqs: List<Rs2QuestVarReq> = emptyList(),
    var varbitReqs: List<Rs2QuestVarReq> = emptyList(),
    override var params: MutableMap<Int, Any>? = null
) : Definition, MutableParameterized
