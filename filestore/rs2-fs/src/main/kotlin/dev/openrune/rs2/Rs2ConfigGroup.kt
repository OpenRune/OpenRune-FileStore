package dev.openrune.rs2

/**
 * Archive/group ids within [Rs2Index.CONFIG] (index 2).
 *
 * Sourced from [zwyz/rs3-cache](https://github.com/zwyz/rs3-cache)'s
 * `Js5ConfigGroup` enum. Some entries (LOC, ENUM, NPC, OBJ, SEQ, SPOT, STRUCT)
 * also exist as their own promoted top-level index in RS3 - see
 * [Rs2Index.CONFIG_LOC] and friends - so treat these as the legacy/fallback
 * location for older cache revisions rather than authoritative for current
 * RS3 caches.
 *
 * The VAR_* block (60-80) is a newer var storage scheme that appears to
 * supersede VARBITTYPE/VARPLAYERTYPE/VARCLIENTTYPE (14/16/19) in later
 * revisions; both are kept here since either may be authoritative depending
 * on cache revision.
 */
object Rs2ConfigGroup {
    const val FLUTYPE = 1
    const val HUNTTYPE = 2
    const val IDKTYPE = 3
    const val FLOTYPE = 4
    const val INVTYPE = 5
    const val LOCTYPE = 6
    const val MESANIMTYPE = 7
    const val ENUMTYPE = 8
    const val NPCTYPE = 9
    const val OBJTYPE = 10
    const val PARAMTYPE = 11
    const val SEQTYPE = 12
    const val SPOTTYPE = 13
    const val VARBITTYPE = 14
    const val VARCLIENTSTRTYPE = 15
    const val VARPLAYERTYPE = 16
    const val CATEGORY = 17
    const val AREATYPE = 18
    const val VARCLIENTTYPE = 19
    const val VAROBJTYPE = 20
    const val VARSHAREDTYPE = 22
    const val VARSHAREDSTRTYPE = 23
    const val VARNPCTYPE = 24
    const val VARNPCBITTYPE = 25
    const val STRUCTTYPE = 26
    const val CHATPHRASETYPE = 27
    const val CHATCATTYPE = 28
    const val SKYBOXTYPE = 29
    const val SKYDECORTYPE = 30
    const val LIGHTTYPE = 31
    const val BASTYPE = 32
    const val CURSORTYPE = 33
    const val MSITYPE = 34
    const val QUESTTYPE = 35
    const val MELTYPE = 36
    const val DBTABLETYPE = 40
    const val DBROWTYPE = 41
    const val CONTROLLERTYPE = 42
    const val HITMARKTYPE = 46
    const val VARCLANTYPE = 47
    const val ITEMCODETYPE = 48
    const val CATEGORYTYPE = 49

    const val VAR_PLAYER = 60
    const val VAR_NPC = 61
    const val VAR_CLIENT = 62
    const val VAR_WORLD = 63
    const val VAR_REGION = 64
    const val VAR_OBJECT = 65
    const val VAR_CLAN = 66
    const val VAR_CLAN_SETTING = 67
    const val VAR_CONTROLLER = 68
    const val VAR_BIT = 69
    const val GAMELOGEVENT = 70
    const val HEADBARTYPE = 72
    const val VAR_GLOBAL = 75
    const val WATERTYPE = 76
    const val SEQGROUPTYPE = 77
    const val VAR_PLAYER_GROUP = 80
    const val WORLDAREATYPE = 83
    const val ACHIEVEMENTTYPE = 85
}
