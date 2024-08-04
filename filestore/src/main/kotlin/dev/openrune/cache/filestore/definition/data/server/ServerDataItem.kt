package dev.openrune.cache.filestore.definition.data.server

data class ServerDataItem(
    var quest_item : Boolean = false,
    var weapon : Weapon = Weapon()
) {
    data class Weapon(var attack_stab : Int = -1)
}
