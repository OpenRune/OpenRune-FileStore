package dev.openrune.definition

enum class Js5GameValGroup(val id: Int, val groupName: String) {
    OBJTYPES(0, "items"),
    NPCTYPES(1, "npcs"),
    INVTYPES(2, "inv"),
    VARPTYPES(3, "varp"),
    VARBITTYPES(4, "varbits"),
    LOCTYPES(6, "objects"),
    SEQTYPES(7, "sequences"),
    SPOTTYPES(8, "spotanims"),
    ROWTYPES(9, "rowtypes"),
    TABLETYPES(10, "tables"),
    SOUNDTYPES(11, "sounds"),
    SPRITETYPES(12, "sprites"),
    IFTYPES(13, "components");

    companion object {
        private val idMap = values().associateBy { it.id }

        fun fromId(id: Int): Js5GameValGroup =
            idMap[id] ?: error("Unknown group type: $id")
    }

    override fun toString(): String = groupName
}