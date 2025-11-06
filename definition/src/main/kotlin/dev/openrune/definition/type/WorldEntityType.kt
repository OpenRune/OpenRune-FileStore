package dev.openrune.definition.type

import dev.openrune.definition.Definition

enum class WorldInteractTarget(val id: Int) {
    WORLD(0),
    CONTENTS(1),
    BOTH(2),
    NONE(3),
    UNKNOWN(3);

    companion object {
        fun fromId(id: Int) = WorldInteractTarget.entries.firstOrNull { it.id == id } ?: UNKNOWN
    }
}

enum class WorldInteractMode(val id: Int) {
    NONE(0),
    EXAMINE(1),
    ALL(2),
    UNKNOWN(3);

    companion object {
        fun fromId(id: Int) = WorldInteractMode.entries.firstOrNull { it.id == id } ?: UNKNOWN
    }
}

data class WorldEntityType(
    override var id: Int = -1,
    var name: String = "null",
    var options: MutableList<String?> = mutableListOf(null, null, null, null, null),
    var active: Boolean = false,
    var mainX: Int = 0,
    var mainZ: Int = 0,
    var boundsOffsetX: Int = 0,
    var boundsOffsetZ: Int = 0,
    var boundSizeZ: Int = 0,
    var boundsSizeZ: Int = 0,
    var anim: Int = -1,
    var mainLevel: Int = 0,
    var interactTarget: WorldInteractTarget = WorldInteractTarget.UNKNOWN,
    var interactContentsMode: WorldInteractMode = WorldInteractMode.UNKNOWN,
    var minimapIcon: Int = -1,
    var rgb: Int = 39188
) : Definition