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
 * instead encoded in a range no real hook argument uses and swapped for the merged packed id in
 * [PackIfType] after every overlay component has been placed.
 *
 * The range is bounded on both sides: hook arguments also carry the client's `event_*` placeholders
 * (`-2147483648` upwards, filled in by the client when the hook fires), which sit far below [BASE]
 * and must pass through untouched.
 */
object SelfRef {
    private const val BASE = -0x100000

    /** Component indexes are 16-bit, so no placeholder ever encodes below this. */
    private const val LOWEST = BASE - 0xFFFF

    fun encode(overlayIndex: Int): Int {
        require(overlayIndex in 0..0xFFFF) { "component index $overlayIndex is out of range" }
        return BASE - overlayIndex
    }

    fun decode(value: Int): Int? = if (value in LOWEST..BASE) BASE - value else null
}
