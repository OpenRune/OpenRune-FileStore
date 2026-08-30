package dev.openrune.cache.gameval

/**
 * A [List] of [GameValElement] that also carries an id index.
 *
 * Callers look elements up by id far more often than they iterate, and the plain-list lookups
 * (`firstOrNull { it.id == id }`, `filterIsInstance<T>().firstOrNull { ... }`) are linear and, in the
 * `filterIsInstance` case, allocate a full copy of the list on every call. Decoding interfaces does
 * one such lookup per component, so that cost is quadratic in the size of the gameval group.
 *
 * It behaves as an ordinary list everywhere else, so existing code keeps working unchanged.
 */
class GameValList(private val elements: List<GameValElement>) : List<GameValElement> by elements {

    private val byId: Map<Int, GameValElement> = HashMap<Int, GameValElement>(
        if (elements.size < 3) 4 else (elements.size / 0.75f).toInt() + 1
    ).apply {
        // First-wins, matching the `firstOrNull` semantics this replaces.
        for (element in elements) {
            putIfAbsent(element.id, element)
        }
    }

    /** The first element with [id], or null when no element in this group uses it. */
    fun first(id: Int): GameValElement? = byId[id]

    override fun equals(other: Any?): Boolean = elements == other

    override fun hashCode(): Int = elements.hashCode()

    override fun toString(): String = elements.toString()
}
