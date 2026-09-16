package dev.openrune.cache.tools.iftype.dsl

data class Placement(val anchor: String, val before: Boolean)

object InterfacePlacements {
    private val byId = mutableMapOf<Int, Map<String, Placement>>()

    fun register(interfaceId: Int, placements: Map<String, Placement>) {
        if (placements.isEmpty()) return
        byId[interfaceId] = placements
    }

    fun take(interfaceId: Int): Map<String, Placement> = byId.remove(interfaceId).orEmpty()
}
