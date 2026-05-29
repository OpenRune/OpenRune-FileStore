package dev.openrune.cache.worldmap.worldmap.providers

/**
 * @author Kris | 18/08/2022
 */
interface ObjectProvider {
    /**
     * Obtains the map scene id of a given object config.
     * Map scenes are used to draw small sprites such as trees and rocks on the world map.
     * The scene is defined by opcode 68, and should default to -1 if undefined.
     */
    fun getMapSceneId(id: Int): Int

    /**
     * Obtains the map icon id of a given object config.
     * Map icons are used to draw small sprites such as shop icons and quest icons.
     * The icon is defined by opcode 82, and should default to -1 if undefined.
     *
     * It should also be noted that the world map transforms the object to match up with current varbit value.
     * It is up to the developer here to return the transformed version if they so choose.
     */
    fun getMapIconId(id: Int): Int

    /**
     * Obtains the boundary type of the object config.
     * The state defines whether walls should render as red or white on the world map.
     * The state is represented by opcode 19, and requires the post-process function to be properly applied on it,
     * which will modify the state based on the object's click options.
     */
    fun getBoundaryType(id: Int): Int
}
