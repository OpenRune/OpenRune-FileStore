package dev.openrune.cache.tools.iftype.dsl

/**
 * Parent-by-name registrations for `inherit()` overlays.
 *
 * A fresh interface knows every parent index at build time, but an overlay merged onto a cache
 * base does not: `parent("hud_container_front")` names a component that only exists in the base,
 * and a child nested under a *new* overlay layer is numbered by the overlay before the merge
 * assigns the final slot. Both are resolved by name in [PackIfType] once the base is loaded.
 */
object InterfaceParents {
    private val byId = mutableMapOf<Int, Map<String, String>>()

    fun register(interfaceId: Int, parentByName: Map<String, String>) {
        if (parentByName.isEmpty()) return
        byId[interfaceId] = parentByName
    }

    fun take(interfaceId: Int): Map<String, String> = byId.remove(interfaceId).orEmpty()
}

/**
 * Placeholder encoding for `component.<self>:<name>` references inside an `inherit()` overlay.
 *
 * The overlay shares its id with the base, so an overlay-local index packed the normal way is
 * indistinguishable from a reference to the base component in that slot. Self references are
 * instead encoded far below any legal hook argument and swapped for the merged packed id in
 * [PackIfType] after every overlay component has been placed.
 */
object SelfRef {
    private const val BASE = -0x100000

    fun encode(overlayIndex: Int): Int = BASE - overlayIndex

    fun decode(value: Int): Int? = if (value <= BASE) BASE - value else null
}
