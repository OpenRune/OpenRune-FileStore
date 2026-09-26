package dev.openrune.definition.type

import dev.openrune.definition.Definition

data class Rs2AchievementDesc(val type: Int, val text: String)

data class Rs2AchievementStatReq(val group: Int, val level: Int, val text: String, val stats: List<Int>)

data class Rs2AchievementVarReq(val group: Int, val value: Int, val text: String, val variables: List<Int>)

data class Rs2AchievementRef(val group: Int, val id: Int)

data class Rs2AchievementTestBit(val group: Int, val variable: Int, val bit: Int, val text: String, val expected: Int)

data class Rs2AchievementTally(val a: Int, val b: Int, val c: Int)

data class Rs2AchievementType(
    override var id: Int = -1,
    var name: String? = null,
    var descriptions: List<Rs2AchievementDesc> = emptyList(),
    var category: Int = -1,
    var sprite: Int? = null,
    var runescore: Int = 0,
    var graceDay: Int = 0,
    var reward: String? = null,
    var statPrereqs: List<Rs2AchievementStatReq> = emptyList(),
    var varpPrereqs: List<Rs2AchievementVarReq> = emptyList(),
    var varbitPrereqs: List<Rs2AchievementVarReq> = emptyList(),
    var achievementPrereqs: List<Rs2AchievementRef> = emptyList(),
    var statReqs: List<Rs2AchievementStatReq> = emptyList(),
    var varpReqs: List<Rs2AchievementVarReq> = emptyList(),
    var varbitReqs: List<Rs2AchievementVarReq> = emptyList(),
    var achievementReqs: List<Rs2AchievementRef> = emptyList(),
    var subCategory: Int = -1,
    var locked: Boolean = false,
    var hide: Int = 0,
    var members: Boolean = true,
    var questPrereqs: List<Rs2AchievementRef> = emptyList(),
    var questReqs: List<Rs2AchievementRef> = emptyList(),
    var varpTestBitPrereqs: List<Rs2AchievementTestBit> = emptyList(),
    var varpTestBitReqs: List<Rs2AchievementTestBit> = emptyList(),
    var varbitTestBitPrereqs: List<Rs2AchievementTestBit> = emptyList(),
    var varbitTestBitReqs: List<Rs2AchievementTestBit> = emptyList(),
    var wideVarbits: Boolean = false,
    var dbRow: Int? = null,
    var checklist: Boolean = false,
    var prereqItemsToComplete: List<Int> = emptyList(),
    var numPrereqGroupsToComplete: Int = 0,
    var reqGroupItemsToComplete: List<Int> = emptyList(),
    var numReqGroupsToComplete: Int = 0,
    var tally: Rs2AchievementTally? = null
) : Definition
