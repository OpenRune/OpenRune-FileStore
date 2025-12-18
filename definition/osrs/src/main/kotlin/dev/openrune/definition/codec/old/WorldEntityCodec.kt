package dev.openrune.definition.codec.old

import dev.openrune.definition.DefinitionCodec
import dev.openrune.definition.type.WorldEntityType
import dev.openrune.definition.type.WorldInteractMode
import dev.openrune.definition.type.WorldInteractTarget
import dev.openrune.definition.util.readNullableLargeSmart
import dev.openrune.definition.util.readString
import dev.openrune.definition.util.writeNullableLargeSmartCorrect
import dev.openrune.definition.util.writeString
import io.netty.buffer.ByteBuf

class WorldEntityCodec : DefinitionCodec<WorldEntityType> {
    override fun WorldEntityType.read(opcode: Int, buffer: ByteBuf) {
        when (opcode) {
            2 -> mainLevel = buffer.readUnsignedByte().toInt()
            4 -> mainX = buffer.readShort().toInt()
            5 -> mainZ = buffer.readShort().toInt()
            6 -> boundsOffsetX = buffer.readShort().toInt()
            7 -> boundsOffsetZ = buffer.readShort().toInt()
            8 -> boundSizeZ = buffer.readUnsignedShort()
            9 -> boundsSizeZ = buffer.readUnsignedShort()
            12 -> name = buffer.readString()
            14 -> active = true
            in 15..19 -> {
                val index = opcode - 15
                options[index] = buffer.readString()
                active = true
            }
            20 -> buffer.readUnsignedShort()
            23 -> interactTarget = WorldInteractTarget.Companion.fromId(buffer.readUnsignedByte().toInt())
            24 -> interactContentsMode = WorldInteractMode.Companion.fromId(buffer.readUnsignedByte().toInt())
            25 -> anim = buffer.readUnsignedShort()
            26 -> minimapIcon = buffer.readNullableLargeSmart()
            27 -> rgb = buffer.readUnsignedShort()
        }
    }


    override fun ByteBuf.encode(definition: WorldEntityType) {
        if (definition.mainLevel != 0) {
            writeByte(2)
            writeByte(definition.mainLevel)
        }

        if (definition.mainX != 0) {
            writeByte(4)
            writeShort(definition.mainX)
        }

        if (definition.mainZ != 0) {
            writeByte(5)
            writeShort(definition.mainZ)
        }

        if (definition.boundsOffsetX != 0) {
            writeByte(6)
            writeShort(definition.boundsOffsetX)
        }

        if (definition.boundsOffsetZ != 0) {
            writeByte(7)
            writeShort(definition.boundsOffsetZ)
        }

        if (definition.boundSizeZ != 0) {
            writeByte(8)
            writeShort(definition.boundSizeZ)
        }

        if (definition.boundsSizeZ != 0) {
            writeByte(9)
            writeShort(definition.boundsSizeZ)
        }

        if (definition.name != "null") {
            writeByte(12)
            writeString(definition.name)
        }

        if (definition.active) {
            writeByte(14)
        }

        definition.options.forEachIndexed { index, option ->
            if (option != null) {
                writeByte(15 + index)
                writeString(option)
            }
        }

        if (definition.interactTarget != WorldInteractTarget.UNKNOWN) {
            writeByte(23)
            writeByte(definition.interactTarget.id)
        }

        if (definition.interactContentsMode != WorldInteractMode.UNKNOWN) {
            writeByte(24)
            writeByte(definition.interactContentsMode.id)
        }

        if (definition.anim != -1) {
            writeByte(25)
            writeShort(definition.anim)
        }

        if (definition.minimapIcon != -1) {
            writeByte(26)
            writeNullableLargeSmartCorrect(definition.minimapIcon)
        }

        if (definition.rgb != 39188) {
            writeByte(27)
            writeShort(definition.rgb)
        }

        writeByte(0)
    }

    override fun createDefinition() = WorldEntityType()
}