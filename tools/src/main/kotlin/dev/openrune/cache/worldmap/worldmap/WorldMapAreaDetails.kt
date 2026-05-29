package dev.openrune.cache.worldmap.worldmap

import dev.openrune.cache.worldmap.worldmap.utils.Coordinate
import dev.openrune.definition.constants.ConstantProvider
import dev.openrune.definition.util.readString
import dev.openrune.definition.util.writeString
import io.netty.buffer.ByteBuf
import java.awt.Color
import java.util.LinkedList

/**
 * @author Kris | 13/08/2022
 */
data class WorldMapAreaDetails(
    val id: Int,
    /**
     * The internal name of the file, used to look up this specific area by name from the cache.
     */
    val internalName: String,

    /**
     * The name that's shown in the selection drop-down on the world map interface.
     */
    val displayName: String,

    /**
     * The origin coordinate where the world map opens when viewing the specific section.
     */
    val origin: Coordinate,

    /**
     * The colour used to fill the areas of the defined world map rectangle that do not come with any map data
     * Majority of the map uses colour (0, 0, 0).
     * Braindeath island uses (255, 255, 255), but as the entire map is filled with map data, the white colour is not visible anywhere.
     * and the tutorial island uses (125, 144, 185), which is the colour of the water.
     */
    val backgroundColour: Int,

    /**
     * The zoom level of the map when opening the given section. Possible values include 50, 75, 100, 150 and 200.
     * The zoom level 150 is never used by any of the maps in OSRS.
     */
    val zoom: Int,

    /**
     * A list of sections definitions that make up the given map view.
     * These sections make up the rectangles that the world map will render the map data for.
     * Certain sections allow the world map to copy map data from one area and show it in a completely different place.
     */
    val sections: List<WorldMapSection>,

    /**
     * Whether the map area is part of the main surface world map.
     * This particular area will be opened if the player opens the world map while positioned somewhere that doesn't support
     * any world map areas.
     */
    val isMain: Boolean = internalName == "main"
) {

    private fun verify() {
        require(sections.size <= UByte.MAX_VALUE.toInt())
    }

    fun encode(buffer: ByteBuf) {
        verify()
        buffer.writeString(internalName)
        buffer.writeString(displayName)
        buffer.writeInt(origin.packedCoord)
        buffer.writeInt(backgroundColour)
        buffer.writeByte(1)
        buffer.writeBoolean(isMain)
        buffer.writeByte(zoom)
        buffer.writeByte(sections.size)
        for (section in sections) {
            section.verify()
            buffer.writeByte(section.type.id)
            section.encode(buffer)
        }
    }

    override fun toString(): String {
        val builder = StringBuilder(1000)
        builder.append("WorldMapArea:").appendLine()
        builder.append("\tinternalName: $internalName").appendLine()
        builder.append("\tdisplayName: $displayName").appendLine()
        builder.append("\torigin: $origin").appendLine()
        val backgroundColour = Color(backgroundColour)
        builder.append("\tbackgroundColour: ${"#%02X%02X%02X".format(backgroundColour.red, backgroundColour.green, backgroundColour.blue)}").appendLine()
        builder.append("\tisMain: $isMain").appendLine()
        builder.append("\tzoom: $zoom").appendLine()
        builder.append("\tsections: ").appendLine()
        for (section in sections) {
            val lines = section.toString().lineSequence()
            for (line in lines) {
                builder.append("\t\t$line").appendLine()
            }
        }
        return builder.toString()
    }

    companion object {
        fun construct(
            rscmName: String,
            displayName: String,
            origin: Coordinate,
            backgroundColour: Int,
            zoom: Int,
            sections: List<WorldMapSection>,
            isMain: Boolean = rscmName == "worldmap.main"
        ): WorldMapAreaDetails {
            val id = ConstantProvider.getMapping(rscmName)
            val internalName = rscmName.replace("worldmap.", "")
            return WorldMapAreaDetails(id, internalName, displayName, origin, backgroundColour, zoom, sections, isMain)
        }
        fun decode(id: Int, buffer: ByteBuf): WorldMapAreaDetails {
            val internalName = buffer.readString()
            val displayName = buffer.readString()
            val origin = Coordinate(buffer.readInt())
            val backgroundColor = buffer.readInt()
            buffer.readUnsignedByte() // Always a value of one
            val isMain = buffer.readUnsignedByte().toInt() == 1
            val zoom = buffer.readUnsignedByte().toInt()
            val count = buffer.readUnsignedByte()
            val sections = LinkedList<WorldMapSection>()
            for (i in 0 until count) {
                sections.add(WorldMapSection.decode(buffer))
            }
            return WorldMapAreaDetails(
                id,
                internalName,
                displayName,
                origin,
                backgroundColor,
                zoom,
                sections,
                isMain
            )
        }
    }
}
