package dev.openrune.definition.type

import dev.openrune.definition.type.builders.WorldEntityTypeBuilder

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

/**
 * A loaded world entity definition. Immutable apart from [id] (which the load machinery
 * assigns): decoding builds one through [WorldEntityTypeBuilder], and everything after that
 * only reads.
 */
data class WorldEntityType(
    override var id: Int = -1,
    val name: String = "null",
    val options: List<String?> = listOf(null, null, null, null, null),
    val active: Boolean = false,
    val mainX: Int = 0,
    val mainZ: Int = 0,
    val boundsOffsetX: Int = 0,
    val boundsOffsetZ: Int = 0,
    val boundSizeZ: Int = 0,
    val boundsSizeZ: Int = 0,
    val anim: Int = -1,
    val mainLevel: Int = 0,
    val interactTarget: WorldInteractTarget = WorldInteractTarget.UNKNOWN,
    val interactContentsMode: WorldInteractMode = WorldInteractMode.UNKNOWN,
    val minimapIcon: Int = -1,
    val rgb: Int = 39188
) : Definition {

    /** A mutable copy of this definition, for tools that need to edit and re-pack it. */
    fun toBuilder(): WorldEntityTypeBuilder = WorldEntityTypeBuilder.from(this)
}