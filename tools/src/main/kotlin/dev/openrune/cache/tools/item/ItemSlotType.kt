package dev.openrune.cache.tools.item

enum class ItemSlotType(val type: String, val slot: Int, val override1: Int = -1, val override2: Int = -1) {
    FULLHELM("fullhelm", 0, 8, 11),
    MEDHELM("medhelm", 0, 8,-1),
    MASK("mask", 0, 11,-1),

    CAPE("cape", 1),
    AMULET("amulet", 2),
    AMMO("ammo", 13),
    WEAPON("weapon", 3),
    SHIELD("shield", 5),
    PLATEBODY("platebody", 4,6,-1),
    CHAINBODY("chainbody", 4),
    LEGS("legs", 7),
    SKIRT("skirt", 7),
    BOOTS("boots", 10),
    RING("ring", 12);

    companion object {
        fun fetchTypes() = entries.map { it.type }.toTypedArray()
        fun fetchType(type: String): ItemSlotType? = entries.find { it.type == type }
    }

}