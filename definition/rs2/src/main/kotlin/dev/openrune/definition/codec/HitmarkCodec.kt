package dev.openrune.definition.codec

import dev.openrune.definition.type.HitmarkType
import dev.openrune.definition.util.readNullableLargeSmartCorrect
import dev.openrune.definition.util.readPrefixedStringCP
import dev.openrune.definition.util.writeNullableLargeSmartCorrect
import dev.openrune.definition.util.writePrefixedString
import dev.openrune.definition.writeTransformBody
import io.netty.buffer.ByteBuf

class HitmarkCodec : Rs2OrderedCodec<HitmarkType> {

    override fun HitmarkType.read(opcode: Int, buffer: ByteBuf) {
        when (opcode) {
            1 -> damageFont = buffer.readNullableLargeSmartCorrect() ?: -1
            2 -> damageColour = buffer.readUnsignedMedium()
            3 -> classGraphic = buffer.readNullableLargeSmartCorrect() ?: -1
            4 -> leftGraphic = buffer.readNullableLargeSmartCorrect() ?: -1
            5 -> middleGraphic = buffer.readNullableLargeSmartCorrect() ?: -1
            6 -> rightGraphic = buffer.readNullableLargeSmartCorrect() ?: -1
            7 -> scrollToOffsetX = buffer.readShort().toInt()
            8 -> damageFormat = buffer.readPrefixedStringCP()
            9 -> stickTime = buffer.readUnsignedShort()
            10 -> scrollToOffsetY = buffer.readShort().toInt()
            11 -> noFadeOut = true
            12 -> replaceMode = buffer.readUnsignedByte().toInt()
            13 -> damageYOffset = buffer.readShort().toInt()
            14 -> fadeOut = buffer.readUnsignedShort()
            16 -> {
                graphicOffsetX = buffer.readShort().toInt()
                graphicOffsetY = buffer.readShort().toInt()
            }

            17, 18 -> readTransforms(buffer, isLast = opcode == 18)
            19 -> damageScaleTo = buffer.readShort().toInt()
            20 -> damageScaleFrom = buffer.readShort().toInt()
            21, 22 -> {
                readTransforms(buffer, isLast = opcode == 22, mediumVarBit = true)
                transformsWideVarBit = true
            }
        }
    }

    override fun ByteBuf.writePayload(opcode: Int, occurrence: Int, definition: HitmarkType) {
        when (opcode) {
            1 -> writeNullableLargeSmartCorrect(definition.damageFont)
            2 -> writeMedium(definition.damageColour!!)
            3 -> writeNullableLargeSmartCorrect(definition.classGraphic)
            4 -> writeNullableLargeSmartCorrect(definition.leftGraphic)
            5 -> writeNullableLargeSmartCorrect(definition.middleGraphic)
            6 -> writeNullableLargeSmartCorrect(definition.rightGraphic)
            7 -> writeShort(definition.scrollToOffsetX!!)
            8 -> writePrefixedString(definition.damageFormat!!)
            9 -> writeShort(definition.stickTime!!)
            10 -> writeShort(definition.scrollToOffsetY!!)
            11 -> Unit
            12 -> writeByte(definition.replaceMode!!)
            13 -> writeShort(definition.damageYOffset!!)
            14 -> writeShort(definition.fadeOut!!)
            16 -> {
                writeShort(definition.graphicOffsetX!!)
                writeShort(definition.graphicOffsetY!!)
            }

            17, 18 -> writeTransformBody(
                this,
                definition.multiVarBit,
                definition.multiVarp,
                definition.multiDefault,
                definition.transforms,
                hasDefault = opcode == 18,
            )

            19 -> writeShort(definition.damageScaleTo!!)
            20 -> writeShort(definition.damageScaleFrom!!)
            21, 22 -> writeTransformBody(
                this,
                definition.multiVarBit,
                definition.multiVarp,
                definition.multiDefault,
                definition.transforms,
                hasDefault = opcode == 22,
                mediumVarBit = true,
            )
        }
    }

    override fun createDefinition() = HitmarkType()
}
