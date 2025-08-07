package dev.openrune.definition

enum class GameValGroupTypes(val id: Int, val groupName: String, val revision : Int = -1) {
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
    IFTYPES_V2(14, "components",232),
    VARCS(15, "varcs",232);

    companion object {
        private val idMap = GameValGroupTypes.entries.associateBy { it.id }

        fun fromId(id: Int): GameValGroupTypes =
            idMap[id] ?: error("Unknown group type: $id")
    }

    override fun toString(): String = groupName
}