package dev.openrune.cache.tools.worldmap.ground

/**
 * @author Kris | 15/08/2022
 */
enum class NeighbourPosition(val id: Int, val index: Int) {
    North(5, 0),
    NorthEast(1, 1),
    East(4, 2),
    SouthEast(6, 3),
    South(3, 4),
    SouthWest(0, 5),
    West(7, 6),
    NorthWest(2, 7);
}
