package dev.openrune.definition.codec

import dev.openrune.definition.revisionIsOrAfter
import dev.openrune.definition.type.FloorOverlayType
import dev.openrune.definition.util.readString
import dev.openrune.definition.util.readUnsignedShortOrNull
import dev.openrune.definition.util.writeString
import io.netty.buffer.ByteBuf

class FloorOverlayCodec(private val revision: Int) : Rs2OrderedCodec<FloorOverlayType> {

    private val wideMaterial get() = revisionIsOrAfter(revision, 400)

    override fun FloorOverlayType.read(opcode: Int, buffer: ByteBuf) {
        when (opcode) {
            1 -> colour = buffer.readUnsignedMedium()
            2 -> material = buffer.readUnsignedByte().toInt()
            3 -> if (wideMaterial) {
                material = buffer.readUnsignedShortOrNull() ?: -1
                materialWide = true
            } else {
                overlay = true
            }

            5 -> occlude = false
            6 -> debugName = buffer.readString()
            7 -> mapColour = buffer.readUnsignedMedium()
            8 -> unknown8 = true
            9 -> textureScale = buffer.readUnsignedShort()
            10 -> hardShadow = false
            11 -> priority = buffer.readUnsignedByte().toInt()
            12 -> smoothEdges = true
            13 -> waterFogColour = buffer.readUnsignedMedium()
            14 -> waterFogScale = buffer.readUnsignedByte().toInt()
            15 -> unknown15 = buffer.readUnsignedShort()
            16 -> waterFogOffset = buffer.readUnsignedByte().toInt()
            20 -> waterFogUnknownA = buffer.readUnsignedShort()
            21 -> waterFogUnknownB = buffer.readUnsignedByte().toInt()
            22 -> waterFogUnknownC = buffer.readUnsignedShort()
        }
    }

    override fun ByteBuf.writePayload(opcode: Int, occurrence: Int, definition: FloorOverlayType) {
        when (opcode) {
            1 -> writeMedium(definition.colour)
            2 -> writeByte(definition.material!!)
            3 -> if (definition.materialWide) writeShort(definition.material!!) else Unit
            5 -> Unit
            6 -> writeString(definition.debugName)
            7 -> writeMedium(definition.mapColour!!)
            8 -> Unit
            9 -> writeShort(definition.textureScale!!)
            10 -> Unit
            11 -> writeByte(definition.priority!!)
            12 -> Unit
            13 -> writeMedium(definition.waterFogColour!!)
            14 -> writeByte(definition.waterFogScale!!)
            15 -> writeShort(definition.unknown15!!)
            16 -> writeByte(definition.waterFogOffset!!)
            20 -> writeShort(definition.waterFogUnknownA!!)
            21 -> writeByte(definition.waterFogUnknownB!!)
            22 -> writeShort(definition.waterFogUnknownC!!)
        }
    }

    override fun createDefinition() = FloorOverlayType()
}
