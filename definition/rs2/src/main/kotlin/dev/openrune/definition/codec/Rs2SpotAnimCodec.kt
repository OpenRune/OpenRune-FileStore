package dev.openrune.definition.codec

import dev.openrune.definition.revisionIsOrAfter
import dev.openrune.definition.writeRecolourPairs
import dev.openrune.definition.type.Rs2SpotAnimType
import dev.openrune.definition.util.readNullableLargeSmartCorrect
import dev.openrune.definition.util.readUnsignedShortOrNull
import dev.openrune.definition.util.writeNullableLargeSmartCorrect
import io.netty.buffer.ByteBuf

class Rs2SpotAnimCodec(private val revision: Int) : Rs2OrderedCodec<Rs2SpotAnimType> {

    private val packedRecolours get() = revisionIsOrAfter(revision, 465)
    private val wideModels get() = revisionIsOrAfter(revision, 681)

    override fun Rs2SpotAnimType.read(opcode: Int, buffer: ByteBuf) {
        check(packedRecolours) { "RS3 spotanims before revision 465 are not supported (got $revision)" }
        when {
            opcode == 1 -> model =
                if (wideModels) buffer.readNullableLargeSmartCorrect() ?: -1 else buffer.readUnsignedShortOrNull() ?: -1

            opcode == 2 -> anim = buffer.readNullableLargeSmartCorrect() ?: -1
            opcode == 3 -> hasAlpha = true
            opcode == 4 -> resizeHorizontal = buffer.readUnsignedShort()
            opcode == 5 -> resizeVertical = buffer.readUnsignedShort()
            opcode == 6 -> rotation = buffer.readUnsignedShort()
            opcode == 7 -> ambient = buffer.readUnsignedByte().toInt()
            opcode == 8 -> contrast = buffer.readUnsignedByte().toInt()
            opcode == 9 -> hillChangeRotate = true
            opcode == 10 -> allowLoop = true
            opcode == 15 -> hillChangeRotateShort = buffer.readUnsignedShort()
            opcode == 16 -> hillChangeRotateInt = buffer.readInt()
            opcode == 40 -> readColours(buffer)
            opcode == 41 -> readTextures(buffer)
            opcode == 44 -> recolourMask = buffer.readUnsignedShort()
            opcode == 45 -> textureMask = buffer.readUnsignedShort()
            opcode == 46 -> castsShadows = false
            isLegacyColourSlot(opcode) -> readLegacyColourSlot(opcode, buffer)
        }
    }

    override fun ByteBuf.writePayload(opcode: Int, occurrence: Int, definition: Rs2SpotAnimType) {
        when {
            opcode == 1 -> {
                val model = definition.model!!
                if (wideModels) writeNullableLargeSmartCorrect(model) else writeShort(model)
            }

            opcode == 2 -> writeNullableLargeSmartCorrect(definition.anim!!)
            opcode == 3 -> Unit
            opcode == 4 -> writeShort(definition.resizeHorizontal!!)
            opcode == 5 -> writeShort(definition.resizeVertical!!)
            opcode == 6 -> writeShort(definition.rotation!!)
            opcode == 7 -> writeByte(definition.ambient!!)
            opcode == 8 -> writeByte(definition.contrast!!)
            opcode == 9 -> Unit
            opcode == 10 -> Unit
            opcode == 15 -> writeShort(definition.hillChangeRotateShort!!)
            opcode == 16 -> writeInt(definition.hillChangeRotateInt!!)
            opcode == 40 -> writeRecolourPairs(definition.originalColours, definition.modifiedColours)
            opcode == 41 -> writeRecolourPairs(definition.originalTextureColours, definition.modifiedTextureColours)
            opcode == 44 -> writeShort(definition.recolourMask!!)
            opcode == 45 -> writeShort(definition.textureMask!!)
            opcode == 46 -> Unit
            definition.isLegacyColourSlot(opcode) -> writeShort(definition.legacyColourSlots.getValue(opcode))
        }
    }

    override fun createDefinition() = Rs2SpotAnimType()
}
