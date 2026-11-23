package dev.openrune.definition.codec

import dev.openrune.definition.type.WorldAreaType
import io.netty.buffer.ByteBuf

class WorldAreaCodec : Rs2OrderedCodec<WorldAreaType> {

    override fun WorldAreaType.read(opcode: Int, buffer: ByteBuf) {
        when (opcode) {
            2 -> colour = buffer.readUnsignedMedium()
            3 -> {
                impostorSquareFrom = buffer.readInt()
                impostorSquareTo = buffer.readInt()
            }

            4 -> {
                impostorZoneCoord = buffer.readInt()
                impostorZoneTemplate = buffer.readInt()
            }
        }
    }

    override fun ByteBuf.writePayload(opcode: Int, occurrence: Int, definition: WorldAreaType) {
        when (opcode) {
            2 -> writeMedium(definition.colour!!)
            3 -> {
                writeInt(definition.impostorSquareFrom!!)
                writeInt(definition.impostorSquareTo!!)
            }

            4 -> {
                writeInt(definition.impostorZoneCoord!!)
                writeInt(definition.impostorZoneTemplate!!)
            }
        }
    }

    override fun createDefinition() = WorldAreaType()
}
