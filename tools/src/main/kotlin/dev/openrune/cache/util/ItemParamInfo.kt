package dev.openrune.cache.util

enum class ItemParam(
    val id: Int,
    val formattedName: String,
    val isSkillParam: Boolean = false,
    val linkedLevelReqId: Int? = null // for skill params, points to level req param ID
) {
    ATTACK_STAB(0, "attackStab"),
    ATTACK_SLASH(1, "attackSlash"),
    ATTACK_CRUSH(2, "attackCrush"),
    ATTACK_MAGIC(3, "attackMagic"),
    ATTACK_RANGED(4, "attackRanged"),
    DEFENCE_STAB(5, "defenceStab"),
    DEFENCE_SLASH(6, "defenceSlash"),
    DEFENCE_CRUSH(7, "defenceCrush"),
    DEFENCE_MAGIC(8, "defenceMagic"),
    DEFENCE_RANGED(9, "defenceRanged"),
    MELEE_STRENGTH(10, "meleeStrength"),
    PRAYER(11, "prayer"),
    ATTACK_SPEED(14, "attackSpeed"),
    RANGED_STRENGTH(12, "rangedStrength"),
    MAGIC_STRENGTH(299, "magicStrength"),
    RANGED_DAMAGE(189, "rangedDamage"),
    MAGIC_DAMAGE(65, "magicDamage"),
    DEMON_DAMAGE(128, "demonDamage"),
    DEGRADEABLE(346, "degradeable"),
    SILVER_STRENGTH(518, "silverStrength"),
    CORP_BOOST(701, "corpBoost"),
    GOLEM_DAMAGE(1178, "golemDamage"),
    KALPHITE_DAMAGE(1353, "kalphiteDamage"),

    SKILL_1(434, "levelReq1", isSkillParam = true, linkedLevelReqId = 436),
    SKILL_2(435, "levelReq2", isSkillParam = true, linkedLevelReqId = 437),
    SKILL_3(191, "levelReq3", isSkillParam = true, linkedLevelReqId = 613),
    SKILL_4(579, "levelReq4", isSkillParam = true, linkedLevelReqId = 614),
    SKILL_5(610, "levelReq5", isSkillParam = true, linkedLevelReqId = 615),
    SKILL_6(611, "levelReq6", isSkillParam = true, linkedLevelReqId = 616),
    SKILL_7(612, "levelReq7", isSkillParam = true, linkedLevelReqId = 617);

    companion object {
        private val byId = values().associateBy(ItemParam::id)
        fun fromId(id: Int): ItemParam? = byId[id]
    }
}