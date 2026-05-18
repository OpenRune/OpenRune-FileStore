package dev.openrune.cache.tools.worldmap

import dev.openrune.cache.tools.worldmap.utils.Coordinate
import dev.openrune.definition.constants.ConstantProvider
import dev.openrune.definition.util.readString
import dev.openrune.definition.util.writeString
import io.netty.buffer.ByteBuf
import kotlinx.serialization.Serializable
import java.awt.Color

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
     * The colour used to fill the areas of the defined world map rectangle that do not come with any map data.
     */
    val backgroundColour: Int,

    /**
     * The zoom level of the map when opening the given section.
     */
    val zoom: Int,

    /**
     * A list of sections definitions that make up the given map view.
     */
    val sections: List<WorldMapSection>,

    /**
     * Whether the map area is part of the main surface world map.
     */
    val isMain: Boolean = internalName == "main"
) {

    private fun verify() {
        require(sections.size <= UByte.MAX_VALUE.toInt()) {
            "Too many sections: ${sections.size}"
        }
    }

    fun encode(buffer: ByteBuf) {
        verify()

        buffer.writeString(internalName)
        buffer.writeString(displayName)
        buffer.writeInt(origin.packedCoord)
        buffer.writeInt(backgroundColour)

        buffer.writeByte(1) // Unknown constant value

        buffer.writeBoolean(isMain)
        buffer.writeByte(zoom)
        buffer.writeByte(sections.size)

        for (section in sections) {
//            section.verify()
            buffer.writeByte(section.type.id)
            section.encode(buffer)
        }
    }

    override fun toString(): String {
        val colour = Color(backgroundColour)
        val hexColour = "#%02X%02X%02X".format(
            colour.red,
            colour.green,
            colour.blue
        )

        return buildString {
            appendLine("WorldMapAreaDetails(")
            appendLine("    id=$id,")
            appendLine("    internalName=\"$internalName\",")
            appendLine("    displayName=\"$displayName\",")
            appendLine("    origin=$origin,")
            appendLine("    backgroundColour=\"$hexColour\",")
            appendLine("    zoom=$zoom,")
            appendLine("    isMain=$isMain,")
            appendLine("    sections=[")

            sections.forEachIndexed { index, section ->
                val formatted = section.toString()
                    .lineSequence()
                    .joinToString("\n") { "            $it" }

                append(formatted)

                if (index != sections.lastIndex) {
                    append(",")
                }

                appendLine()
            }

            appendLine("    ]")
            append(")")
        }
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
            val internalName = rscmName.removePrefix("worldmap.")

            return WorldMapAreaDetails(
                id = id,
                internalName = internalName,
                displayName = displayName,
                origin = origin,
                backgroundColour = backgroundColour,
                zoom = zoom,
                sections = sections,
                isMain = isMain
            )
        }

        fun decode(id: Int, buffer: ByteBuf): WorldMapAreaDetails {
            return buffer.readWorldMapAreaDetails(id)
        }
    }
}

/**
 * Decodes a [WorldMapAreaDetails] from this [ByteBuf].
 */
fun ByteBuf.readWorldMapAreaDetails(id: Int): WorldMapAreaDetails {
    val internalName = readString()
    val displayName = readString()
    val origin = Coordinate(readInt())
    val backgroundColour = readInt()

    readUnsignedByte() // Always 1

    val isMain = readUnsignedByte().toInt() == 1
    val zoom = readUnsignedByte().toInt()
    val count = readUnsignedByte().toInt()

    val sections = buildList(count) {
        repeat(count) {
            add(WorldMapSection.decode(this@readWorldMapAreaDetails))
        }
    }

    return WorldMapAreaDetails(
        id = id,
        internalName = internalName,
        displayName = displayName,
        origin = origin,
        backgroundColour = backgroundColour,
        zoom = zoom,
        sections = sections,
        isMain = isMain
    )
}