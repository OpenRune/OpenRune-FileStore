package dev.openrune.definition.codec

import dev.openrune.definition.DefinitionCodec
import dev.openrune.definition.revisionIsOrBefore
import dev.openrune.definition.type.MultiSquare
import dev.openrune.definition.type.MultiZone
import dev.openrune.definition.type.SingleSquare
import dev.openrune.definition.type.SingleZone
import dev.openrune.definition.type.WorldMapSectionType
import dev.openrune.definition.type.WorldMapAreaType
import dev.openrune.definition.util.Coord
import dev.openrune.definition.util.readString
import io.netty.buffer.ByteBuf

class WorldMapAreaCodec(val rev : Int) : DefinitionCodec<WorldMapAreaType> {

    override fun WorldMapAreaType.read(opcode: Int, buffer: ByteBuf) {
        this.internalName = buffer.readString()
        this.externalName = buffer.readString()
        this.origin = Coord(buffer.readInt())
        this.backgroundColour = buffer.readInt()
        if (revisionIsOrBefore(rev, 217)) {
            this.fillColour = buffer.readInt()
        }
        buffer.readUnsignedByte()
        this.isMain = buffer.readUnsignedByte().toInt() == 1
        this.zoom = buffer.readUnsignedByte().toInt()

        val count  = buffer.readUnsignedByte()
        val sections : MutableList<WorldMapSectionType> = emptyList<WorldMapSectionType>().toMutableList()
        for (i in 0 until count) {
            val typeId = buffer.readUnsignedByte().toInt()
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

    }

    override fun createDefinition() = WorldMapAreaType()

    override fun readLoop(definition: WorldMapAreaType, buffer: ByteBuf) {
        definition.read(-1, buffer)
    }
}