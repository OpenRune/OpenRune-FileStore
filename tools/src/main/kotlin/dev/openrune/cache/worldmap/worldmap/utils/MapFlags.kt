package dev.openrune.cache.worldmap.worldmap.utils

/**
 * @author Kris | 21/08/2022
 */
internal object MapFlags {
    const val FLAG_BLOCKED = 0x1
    const val FLAG_LINK_BELOW = 0x2
    const val FLAG_REMOVE_ROOF = 0x4
    const val FLAG_VISIBLE_BELOW = 0x8
    const val FLAG_FORCE_HIGH_DETAIL = 0x10
}
