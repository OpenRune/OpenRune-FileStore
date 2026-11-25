package dev.openrune.definition

enum class GameValGroupTypeRS3(
    override val id: Int,
    override val groupName: String,
) : GameValGroup {
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
    JINGLE(69, "jingles"),
    VAR_PLAYER_GROUP(80, "varplayergroup"),
    ACHIEVEMENT(89, "achievement"),
    FONTMETRICS(90, "fontmetrics"),
    STYLESHEET(92, "stylesheet"),
    UI_ANIM_CURVE(96, "uianimcurve"),
    UI_ANIM(97, "uianim"),
    ;

    override val revision: Int get() = -1

    override val index: Int get() = GAMEVAL_INDEX

    val hasVarBitNames: Boolean get() = this in VAR_GROUPS

    override fun toString(): String = groupName

    companion object {
        const val GAMEVAL_INDEX = 67

        private val VAR_GROUPS = setOf(
            VAR_CLAN, VAR_CLAN_SETTING, VAR_CLIENT, VAR_NPC, VAR_OBJECT, VAR_PLAYER, VAR_PLAYER_GROUP,
        )

        private val byId = entries.groupBy { it.id }

        fun fromId(id: Int): GameValGroupTypeRS3? = byId[id]?.first()

        fun allFromId(id: Int): List<GameValGroupTypeRS3> = byId[id].orEmpty()
    }
}

typealias Rs2AssetNameGroup = GameValGroupTypeRS3
