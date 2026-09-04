package dev.openrune.cache.gameval

/**
 * A [List] of [GameValElement] carrying an id index, so [GameValHandler.lookup] and
 * [GameValHandler.lookupAs] are constant time instead of a linear scan per call. Behaves as an
 * ordinary list everywhere else.
 */
class GameValList(private val elements: List<GameValElement>) : List<GameValElement> by elements {

    private val byId: Map<Int, GameValElement> = HashMap<Int, GameValElement>(
        if (elements.size < 3) 4 else (elements.size / 0.75f).toInt() + 1
    ).apply {
        // First-wins.
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
