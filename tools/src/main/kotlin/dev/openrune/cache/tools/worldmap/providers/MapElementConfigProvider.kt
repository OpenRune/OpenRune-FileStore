package dev.openrune.cache.tools.worldmap.providers

/**
 * @author Kris | 22/08/2022
 */
interface MapElementConfigProvider {
    fun getMapElement(id: Int): MapElement
}

interface MapElement {
    val text: String?
    val textSize: Int
    val textColour: Int
    val graphic: Int
    val horizontalAlignment: Int
    val verticalAlignment: Int
}
