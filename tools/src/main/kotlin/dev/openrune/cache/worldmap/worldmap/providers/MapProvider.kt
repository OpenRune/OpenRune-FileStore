package dev.openrune.cache.worldmap.worldmap.providers

import dev.openrune.cache.worldmap.worldmap.utils.Coordinate
import dev.openrune.filesystem.Cache

/**
 * @author Kris | 18/08/2022
 */
data class Mapsquare(val landscape: Landscape, val objects: Collection<WorldMapObject>)

interface MapProvider {
    /**
     * Mapsquare provider. This function should return null if the map does not exist at all(not even a water square).
     * If the landscape, e.g. water, exists, but objects don't, an empty list for objects should be returned instead.
     * It should also be noted that the objects are expected to be in raw cache format, **without** the level coordinate
     * affected by the bridge flag.
     */
    fun getMap(cache: Cache, mapsquareX: Int, mapsquareY: Int): Mapsquare?
}

interface WorldMapObject {
    /**
     * The config id of the given object.
     */
    val id: Int

    /**
     * The shape of the given object, a value from 0 to 22(inclusive).
     */
    val shape: Int

    /**
     * The rotation of the object, a value from 0 to 3(inclusive).
     */
    val rotation: Int

    /**
     * The coordinate of the object. This coordinate can either be local or global, as it gets converted to local anyways.
     */
    val coordinate: Coordinate
}

interface Landscape {
    /**
     * A function to obtain the underlay id for the given local x and y coordinates in the mapsquare.
     * If the underlay does not exist there, a value of -1 should be returned.
     */
    fun getUnderlayId(level: Int, x: Int, y: Int): Int

    /**
     * A function to obtain the overlay id for the given local x and y coordinates in the mapsquare.
     * If the overlay does not exist there, a value of -1 should be returned.
     */
    fun getOverlayId(level: Int, x: Int, y: Int): Int

    /**
     * A function to obtain the overlay shape for the given local x and y coordinates in the mapsquare.
     * If the overlay does not exist there, a value of 0 should be returned.
     */
    fun getOverlayShape(level: Int, x: Int, y: Int): Int

    /**
     * A function to obtain the overlay rotation for the given local x and y coordinates in the mapsquare.
     * If the overlay does not exist there, a value of 0 should be returned.
     */
    fun getOverlayRotation(level: Int, x: Int, y: Int): Int

    /**
     * A function to obtain the flags that dictate how the particular tile behaves.
     * This is necessary to determine if an object should be included from the upper levels if the bridge flag
     * or the "visible from below" flags are enabled.
     */
    fun getFlags(level: Int, x: Int, y: Int): Int
}
