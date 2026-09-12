package dev.openrune.definition.type.builders

import dev.openrune.definition.type.*

class WorldEntityTypeBuilder(var id: Int = -1) {

    var name: String = "null"
    var options: MutableList<String?> = mutableListOf(null, null, null, null, null)
    var active: Boolean = false
    var mainX: Int = 0
    var mainZ: Int = 0
    var boundsOffsetX: Int = 0
    var boundsOffsetZ: Int = 0
    var boundSizeZ: Int = 0
    var boundsSizeZ: Int = 0
    var anim: Int = -1
    var mainLevel: Int = 0
    var interactTarget: WorldInteractTarget = WorldInteractTarget.UNKNOWN
    var interactContentsMode: WorldInteractMode = WorldInteractMode.UNKNOWN
    var minimapIcon: Int = -1
    var rgb: Int = 39188

    fun build(): WorldEntityType = WorldEntityType(
        id = id,
        name = name,
        options = options.toList(),
        active = active,
        mainX = mainX,
        mainZ = mainZ,
        boundsOffsetX = boundsOffsetX,
        boundsOffsetZ = boundsOffsetZ,
        boundSizeZ = boundSizeZ,
        boundsSizeZ = boundsSizeZ,
        anim = anim,
        mainLevel = mainLevel,
        interactTarget = interactTarget,
        interactContentsMode = interactContentsMode,
        minimapIcon = minimapIcon,
        rgb = rgb,
    )

    companion object {
        /** Copies every field of [type] into a fresh builder. List contents are copied too. */
        fun from(type: WorldEntityType): WorldEntityTypeBuilder {
            val builder = WorldEntityTypeBuilder(type.id)
            builder.name = type.name
            builder.options = type.options.toMutableList()
            builder.active = type.active
            builder.mainX = type.mainX
            builder.mainZ = type.mainZ
            builder.boundsOffsetX = type.boundsOffsetX
            builder.boundsOffsetZ = type.boundsOffsetZ
            builder.boundSizeZ = type.boundSizeZ
            builder.boundsSizeZ = type.boundsSizeZ
            builder.anim = type.anim
            builder.mainLevel = type.mainLevel
            builder.interactTarget = type.interactTarget
            builder.interactContentsMode = type.interactContentsMode
            builder.minimapIcon = type.minimapIcon
            builder.rgb = type.rgb
            return builder
        }
    }
}
