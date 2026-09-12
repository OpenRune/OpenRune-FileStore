package dev.openrune

import dev.openrune.definition.codec.WorldMapAreaCodec
import dev.openrune.definition.type.MultiSquare
import dev.openrune.definition.type.MultiZone
import dev.openrune.definition.type.SingleSquare
import dev.openrune.definition.type.SingleZone
import dev.openrune.definition.type.WorldMapAreaType
import dev.openrune.definition.util.Coord
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * The section layouts differ in length (10, 14 and 18 bytes), so reading one with another's layout leaves
 * the reader mid-record rather than throwing straight away. Every test here therefore asserts the whole
 * record was consumed, which is what actually catches a swapped layout.
 */
class WorldMapAreaCodecTest {

    private val codec = WorldMapAreaCodec(238)

    private fun buffer(build: ByteBuf.() -> Unit): ByteBuf = Unpooled.buffer().apply(build)

    private fun ByteBuf.writeCString(value: String) {
        value.forEach { writeByte(it.code) }
        writeByte(0)
    }

    private fun ByteBuf.writeHeader(sections: Int) {
        writeCString("main")
        writeCString("Main Map")
        writeInt(Coord(0, 3200, 3200).packed)
        writeInt(0x00FF00)
        writeInt(-16777216)
        writeByte(1)
        writeByte(1)
        writeByte(50)
        writeByte(sections)
    }

    private fun decode(data: ByteBuf): WorldMapAreaType {
        val definition = codec.loadData(7, data)
        assertEquals(0, data.readableBytes(), "decoder did not consume the whole record")
        return definition
    }

    private fun encode(definition: WorldMapAreaType): ByteArray {
        val out = Unpooled.buffer()
        with(codec) { out.encode(definition) }
        return ByteArray(out.readableBytes()).also { out.readBytes(it) }
    }

    @Test
    fun `decodes the record header`() {
        val definition = decode(buffer { writeHeader(0) })

        assertEquals("main", definition.internalName)
        assertEquals("Main Map", definition.externalName)
        assertEquals(Coord(0, 3200, 3200), definition.origin)
        assertEquals(0x00FF00, definition.backgroundColour)
        assertEquals(-16777216, definition.fillColour)
        assertTrue(definition.isMain)
        assertEquals(50, definition.zoom)
        assertTrue(definition.sections.isEmpty())
    }

    @Test
    fun `decodes a multi mapsquare section`() {
        val definition = decode(buffer {
            writeHeader(1)
            writeByte(0)
            writeByte(0); writeByte(4)
            writeShort(40); writeShort(50); writeShort(44); writeShort(55)
            writeShort(60); writeShort(70); writeShort(64); writeShort(75)
        })

        val section = definition.sections.single() as MultiSquare
        assertEquals(
            MultiSquare(
                level = 0,
                levelsCount = 4,
                sourceMinX = 40,
                sourceMinY = 50,
                sourceMaxX = 44,
                sourceMaxY = 55,
                destinationMinX = 60,
                destinationMinY = 70,
                destinationMaxX = 64,
                destinationMaxY = 75,
            ),
            section,
        )
    }

    /** Ten bytes, not the eighteen the zone layout takes — this is the pairing that was reversed. */
    @Test
    fun `decodes a single mapsquare section`() {
        val definition = decode(buffer {
            writeHeader(1)
            writeByte(1)
            writeByte(1); writeByte(2)
            writeShort(37); writeShort(64)
            writeShort(48); writeShort(80)
        })

        assertEquals(
            SingleSquare(
                level = 1,
                levelsCount = 2,
                sourceX = 37,
                sourceY = 64,
                destinationX = 48,
                destinationY = 80,
            ),
            definition.sections.single() as SingleSquare,
        )
    }

    /** Eighteen bytes: mapsquare shorts with a zone byte pair either side of each. */
    @Test
    fun `decodes a multi zone section`() {
        val definition = decode(buffer {
            writeHeader(1)
            writeByte(2)
            writeByte(0); writeByte(1)
            writeShort(41); writeByte(2); writeByte(5)
            writeShort(52); writeByte(1); writeByte(6)
            writeShort(61); writeByte(3); writeByte(6)
            writeShort(72); writeByte(0); writeByte(5)
        })

        assertEquals(
            MultiZone(
                level = 0,
                levelsCount = 1,
                sourceMapsquareX = 41,
                sourceZoneMinX = 2,
                sourceZoneMaxX = 5,
                sourceMapsquareY = 52,
                sourceZoneMinY = 1,
                sourceZoneMaxY = 6,
                destinationMapsquareX = 61,
                destinationZoneMinX = 3,
                destinationZoneMaxX = 6,
                destinationMapsquareY = 72,
                destinationZoneMinY = 0,
                destinationZoneMaxY = 5,
            ),
            definition.sections.single() as MultiZone,
        )
    }

    @Test
    fun `decodes a single zone section`() {
        val definition = decode(buffer {
            writeHeader(1)
            writeByte(3)
            writeByte(0); writeByte(1)
            writeShort(48); writeByte(7)
            writeShort(48); writeByte(2)
            writeShort(50); writeByte(1)
            writeShort(51); writeByte(4)
        })

        assertEquals(
            SingleZone(
                level = 0,
                levelsCount = 1,
                sourceMapsquareX = 48,
                sourceZoneX = 7,
                sourceMapsquareY = 48,
                sourceZoneY = 2,
                destinationMapsquareX = 50,
                destinationZoneX = 1,
                destinationMapsquareY = 51,
                destinationZoneY = 4,
            ),
            definition.sections.single() as SingleZone,
        )
    }

    @Test
    fun `re-encodes a record with every section type byte for byte`() {
        val original = buffer {
            writeHeader(4)
            // multi mapsquare
            writeByte(0)
            writeByte(0); writeByte(4)
            writeShort(40); writeShort(50); writeShort(44); writeShort(55)
            writeShort(60); writeShort(70); writeShort(64); writeShort(75)
            // single mapsquare
            writeByte(1)
            writeByte(1); writeByte(2)
            writeShort(37); writeShort(64); writeShort(48); writeShort(80)
            // multi zone
            writeByte(2)
            writeByte(0); writeByte(1)
            writeShort(41); writeByte(2); writeByte(5)
            writeShort(52); writeByte(1); writeByte(6)
            writeShort(61); writeByte(3); writeByte(6)
            writeShort(72); writeByte(0); writeByte(5)
            // single zone
            writeByte(3)
            writeByte(0); writeByte(1)
            writeShort(48); writeByte(7)
            writeShort(48); writeByte(2)
            writeShort(50); writeByte(1)
            writeShort(51); writeByte(4)
        }
        val expected = ByteArray(original.readableBytes()).also { original.getBytes(0, it) }

        val definition = decode(original)
        assertEquals(4, definition.sections.size)
        assertArrayEquals(expected, encode(definition))
    }

    @Test
    fun `round trips the re-encoded record back to the same definition`() {
        val definition = WorldMapAreaType(
            id = 12,
            internalName = "zanaris",
            externalName = "Zanaris",
            origin = Coord(0, 2400, 4400),
            backgroundColour = 0,
            fillColour = -16777216,
            isMain = false,
            zoom = 75,
            sections = listOf(
                SingleSquare(0, 1, 37, 64, 48, 80),
                MultiZone(0, 2, 41, 2, 5, 52, 1, 6, 61, 3, 6, 72, 0, 5),
            ),
        )

        val decoded = decode(Unpooled.wrappedBuffer(encode(definition)))

        assertEquals(definition.internalName, decoded.internalName)
        assertEquals(definition.externalName, decoded.externalName)
        assertEquals(definition.origin, decoded.origin)
        assertEquals(definition.backgroundColour, decoded.backgroundColour)
        assertEquals(definition.fillColour, decoded.fillColour)
        assertEquals(definition.isMain, decoded.isMain)
        assertEquals(definition.zoom, decoded.zoom)
        assertEquals(definition.sections, decoded.sections)
    }
}
