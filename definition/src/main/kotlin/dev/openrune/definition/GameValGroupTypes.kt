package dev.openrune.definition

enum class GameValGroupTypesOSRS(
    override val id: Int,
    override val groupName: String,
    override val revision: Int = -1,
) : GameValGroup {
    OBJTYPES(0, "items"),
    NPCTYPES(1, "npcs"),
    INVTYPES(2, "inv"),
    VARPTYPES(3, "varp"),
    VARBITTYPES(4, "varbits"),
    LOCTYPES(6, "objects"),
    SEQTYPES(7, "sequences"),
    SPOTTYPES(8, "spotanims"),
    ROWTYPES(9, "dbrows"),
    TABLETYPES(10, "dbtables"),
    SOUNDTYPES(11, "jingles"),
    SPRITETYPES(12, "sprites"),
    IFTYPES(13, "components"),
    IFTYPES_V2(14, "components", 232),
    VARCS(15, "varcs", 232),
    SYNTHTYPES(16, "synth", 242),
    MIDITYPES(17, "midi", 242),
    JINGLETYPES(18, "jingles", 242),
    AMBIENTSOUNDTYPES(19, "ambientsounds", 242);

    override val index: Int get() = GAMEVAL_INDEX

    companion object {
        const val GAMEVAL_INDEX = 24

        private val idMap = entries.associateBy { it.id }

        fun fromId(id: Int): GameValGroupTypesOSRS =
            idMap[id] ?: error("Unknown group type: $id")
    }

    override fun toString(): String = groupName
}

typealias GameValGroupTypes = GameValGroupTypesOSRS
