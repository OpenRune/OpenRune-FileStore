package dev.openrune.definition

enum class VarDomain(val id: Int) {
    PLAYER(0),
    NPC(1),
    CLIENT(2),
    WORLD(3),
    REGION(4),
    OBJECT(5),
    CLAN(6),
    CLAN_SETTING(7),
    CONTROLLER(8),
    PLAYER_GROUP(9),
    GLOBAL(10),
    ;

    companion object {
        private val byId = entries.associateBy { it.id }

        fun byID(id: Int): VarDomain = byId[id] ?: error("Unknown var domain: $id")
    }
}
