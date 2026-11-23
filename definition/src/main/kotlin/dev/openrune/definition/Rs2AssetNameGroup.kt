package dev.openrune.definition

enum class Rs2AssetNameGroup(val id: Int, val table: String) {
    COMPONENT(0, "component"),
    BAS(5, "bas"),
    CATEGORY(9, "category"),
    CURSOR(12, "cursor"),
    DBROW(14, "dbrow"),
    DBTABLE(15, "dbtable"),
    ENUM(16, "enum"),
    HEADBAR(20, "headbar"),
    HITMARK(21, "hitmark"),
    INTERFACE(24, "interface"),
    INV(25, "inv"),
    LOC(28, "loc"),
    MAPELEMENT(29, "mapelement"),
    MATERIAL(32, "material"),
    MODEL(34, "model"),
    NPC(35, "npc"),
    OBJ(36, "obj"),
    PARAM(37, "param"),
    QUEST(41, "quest"),
    SEQ(44, "seq"),
    GRAPHIC(49, "sprites"),
    STRUCT(50, "struct"),
    VAR_CLAN(55, "varclan"),
    VAR_CLAN_SETTING(56, "varclansetting"),
    VAR_CLIENT(57, "varclient"),
    VAR_NPC(59, "varnpc"),
    VAR_OBJECT(60, "varobj"),
    VAR_PLAYER(61, "varp"),
    SOUND(64, "synth"),

    MIDI(69, "midi"),
    VAR_PLAYER_GROUP(80, "varplayergroup"),
    ACHIEVEMENT(89, "achievement"),
    FONTMETRICS(90, "fontmetrics"),
    STYLESHEET(92, "stylesheet"),
    UI_ANIM_CURVE(96, "uianimcurve"),
    UI_ANIM(97, "uianim"),
    ;

    val hasVarBitNames: Boolean
        get() = this in VAR_GROUPS

    companion object {
        private val VAR_GROUPS = setOf(
            VAR_CLAN, VAR_CLAN_SETTING, VAR_CLIENT, VAR_NPC, VAR_OBJECT, VAR_PLAYER, VAR_PLAYER_GROUP,
        )

        private val byId = entries.associateBy { it.id }

        fun fromId(id: Int): Rs2AssetNameGroup? = byId[id]
    }
}
