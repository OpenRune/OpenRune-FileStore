package dev.openrune.definition.codec

import dev.openrune.definition.DefinitionCodec
import dev.openrune.definition.type.MultiSquare
import dev.openrune.definition.type.MultiZone
import dev.openrune.definition.type.SingleSquare
import dev.openrune.definition.type.SingleZone
import dev.openrune.definition.type.WorldMapSectionType
import dev.openrune.definition.type.WorldMapAreaType
import dev.openrune.definition.type.builders.WorldMapAreaTypeBuilder
import dev.openrune.definition.util.Coord
import dev.openrune.definition.util.readString
import dev.openrune.definition.util.writeString
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled

class WorldMapAreaCodec(val rev : Int) : DefinitionCodec<WorldMapAreaType> {

    override fun WorldMapAreaType.read(opcode: Int, buffer: ByteBuf) =
        error("WorldMapAreaType is immutable; decoding goes through WorldMapAreaTypeBuilder")

    override fun loadData(id: Int, data: ByteBuf?): WorldMapAreaType = decode(id, data)

    override fun loadData(id: Int, data: ByteArray?): WorldMapAreaType =
        decode(id, data?.takeIf { it.isNotEmpty() }?.let { Unpooled.wrappedBuffer(it) })

    /** The record is one fixed layout, not an opcode stream, so it is read in a single pass. */
    private fun decode(id: Int, data: ByteBuf?): WorldMapAreaType {
        val builder = WorldMapAreaTypeBuilder(id)
        if (data != null && data.readableBytes() > 0) {
            try {
                builder.read(data)
            } catch (e: Exception) {
                throw IllegalStateException("Unable to decode WorldMapAreaType [$id]", e)
            }
        }
        return builder.build()
    }

    private fun WorldMapAreaTypeBuilder.read(buffer: ByteBuf) {
        this.internalName = buffer.readString()
        this.externalName = buffer.readString()
        this.origin = Coord(buffer.readInt())
        this.backgroundColour = buffer.readInt()
        // Present in every revision checked (223 and 238 clients both read three ints here).
        this.fillColour = buffer.readInt()
        buffer.readUnsignedByte()
        this.isMain = buffer.readUnsignedByte().toInt() == 1
        this.zoom = buffer.readUnsignedByte().toInt()

        val count  = buffer.readUnsignedByte()
        val sections : MutableList<WorldMapSectionType> = emptyList<WorldMapSectionType>().toMutableList()
        for (i in 0 until count) {
            val typeId = buffer.readUnsignedByte().toInt()
            // Ids map to layouts as the client does it (WorldMapData.method6401): 0 whole mapsquare
            // rectangles, 1 a single mapsquare, 2 zone rectangles, 3 a single zone. Reading 1 and 2 with
            // each other's layout silently overruns the record and fails every area that uses them.
            val section: WorldMapSectionType = when (typeId) {
                0 -> MultiSquare()
                1 -> SingleSquare()
                2 -> MultiZone()
                3 -> SingleZone()
                else -> throw IllegalArgumentException("Unknown section type: $typeId")
            }.also { it.decode(buffer) }
            sections.add(section)
        }
        this.sections = sections

    }

    override fun ByteBuf.encode(definition: WorldMapAreaType) {
        writeString(definition.internalName)
        writeString(definition.externalName)
        writeInt(definition.origin?.packed ?: 0)
        writeInt(definition.backgroundColour)
        writeInt(definition.fillColour)
        // Always one in every cache checked; the client reads and discards it.
        writeByte(1)
        writeByte(if (definition.isMain) 1 else 0)
        writeByte(definition.zoom)
        writeByte(definition.sections.size)
        for (section in definition.sections) {
            writeByte(section.sectionId)
            section.encode(this)
        }
    }

    override fun createDefinition() = WorldMapAreaType()
}
