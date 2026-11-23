package dev.openrune.definition.type

data class Rs2NpcOverride(
    var leading: Int = 0,
    var varBit: Int = -1,
    var varPlayer: Int = -1,
    var mediumVarBit: Boolean = false,
    var flags: Int = 0,
    var models: MutableList<Rs2OverrideGroup<Rs2OverrideModel>> = mutableListOf(),
    var headModels: MutableList<Rs2OverrideGroup<Rs2OverrideModel>> = mutableListOf(),
    var retextures: MutableList<Rs2OverrideGroup<Rs2OverrideRecolour>> = mutableListOf(),
    var recolours: MutableList<Rs2OverrideGroup<Rs2OverrideRecolour>> = mutableListOf(),
    var tints: MutableList<Rs2OverrideTint> = mutableListOf(),
    var default: Int = 0,
)

data class Rs2OverrideGroup<T>(
    var key: Int = 0,
    var entries: MutableList<T> = mutableListOf(),
)

data class Rs2OverrideModel(
    var from: Int = 0,
    var to: Int = 0,
    var model: Int = 0,
    var extra: MutableList<Int> = mutableListOf(),
)

data class Rs2OverrideRecolour(
    var from: Int = 0,
    var to: Int = 0,
    var source: Int = 0,
    var destination: Int = 0,
)

data class Rs2OverrideTint(
    var from: Int = 0,
    var to: Int = 0,
    var hue: Int = 0,
    var saturation: Int = 0,
    var lightness: Int = 0,
    var opacity: Int = 0,
)
