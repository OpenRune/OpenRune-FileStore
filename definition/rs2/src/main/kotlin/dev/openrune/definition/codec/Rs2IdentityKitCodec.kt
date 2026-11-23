package dev.openrune.definition.codec

import dev.openrune.definition.revisionIsOrAfter
import dev.openrune.definition.writeRecolourPairs
import dev.openrune.definition.type.Rs2IdentityKitType
import dev.openrune.definition.util.readNullableLargeSmartCorrect
import dev.openrune.definition.util.readUnsignedShortOrNull
import dev.openrune.definition.util.writeNullableLargeSmartCorrect
import io.netty.buffer.ByteBuf

class Rs2IdentityKitCodec(private val revision: Int) : Rs2OrderedCodec<Rs2IdentityKitType> {

    private val packedRecolours get() = revisionIsOrAfter(revision, 465)
    private val wideModels get() = revisionIsOrAfter(revision, 681)

    private fun ByteBuf.readModel(): Int =
        if (wideModels) readNullableLargeSmartCorrect() ?: -1 else readUnsignedShortOrNull() ?: -1

    private fun ByteBuf.writeModel(value: Int) {
        if (wideModels) writeNullableLargeSmartCorrect(value) else writeShort(value)
    }

    override fun Rs2IdentityKitType.read(opcode: Int, buffer: ByteBuf) {
        check(packedRecolours) { "RS3 identity kits before revision 465 are not supported (got $revision)" }
        when {
            opcode == 1 -> bodyPart = buffer.readUnsignedByte().toInt()
            opcode == 2 -> {
                val count = buffer.readUnsignedByte().toInt()
                models = MutableList(count) { buffer.readModel() }
            }

            opcode == 3 -> disable = true
            opcode == 40 -> readColours(buffer)
            opcode == 41 -> readTextures(buffer)
            opcode == 44 -> recolourMask = buffer.readUnsignedShort()
            opcode == 45 -> textureMask = buffer.readUnsignedShort()
            isLegacyColourSlot(opcode) -> readLegacyColourSlot(opcode, buffer)
            opcode in 60..69 -> heads[opcode] = buffer.readModel()
        }
    }

    override fun ByteBuf.writePayload(opcode: Int, occurrence: Int, definition: Rs2IdentityKitType) {
        when {
            opcode == 1 -> writeByte(definition.bodyPart!!)
            opcode == 2 -> {
                val models = definition.models!!
                writeByte(models.size)
                models.forEach { writeModel(it) }
            }

            opcode == 3 -> Unit
            opcode == 40 -> writeRecolourPairs(definition.originalColours, definition.modifiedColours)
            opcode == 41 -> writeRecolourPairs(definition.originalTextureColours, definition.modifiedTextureColours)
            opcode == 44 -> writeShort(definition.recolourMask!!)
            opcode == 45 -> writeShort(definition.textureMask!!)
            definition.isLegacyColourSlot(opcode) -> writeShort(definition.legacyColourSlots.getValue(opcode))
            opcode in 60..69 -> writeModel(definition.heads.getValue(opcode))
        }
    }

    override fun createDefinition() = Rs2IdentityKitType()
}
