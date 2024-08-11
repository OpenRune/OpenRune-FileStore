package dev.openrune.cache.filestore.definition.encoder

import dev.openrune.cache.filestore.buffer.Writer
import dev.openrune.cache.filestore.definition.ConfigEncoder
import dev.openrune.cache.filestore.definition.data.HitSplatType

class HitSplatEncoder : ConfigEncoder<HitSplatType>() {

    override fun Writer.encode(definition: HitSplatType) {
        if (definition.font != -1) {
            writeByte(1)
            writeShort(definition.font)
        }
        if (definition.textColour != 16777215) {
            writeByte(2)
            writeMedium(definition.textColour)
        }
        if (definition.icon != -1) {
            writeByte(3)
            writeShort(definition.icon)
        }
        if (definition.left != -1) {
            writeByte(4)
            writeShort(definition.left)
        }
        if (definition.middle != -1) {
            writeByte(5)
            writeShort(definition.middle)
        }
        if (definition.right != -1) {
            writeByte(6)
            writeShort(definition.right)
        }
        if (definition.getOffsetX() != 0) {
            writeByte(7)
            writeShort(definition.getOffsetX())
        }
        if (definition.amount != "") {
            writeByte(8)
            writePrefixedString(definition.amount)
        }
        if (definition.getDuration() != 70) {
            writeByte(9)
            writeShort(definition.getDuration())
        }
        if (definition.getOffsetY() != 0) {
            writeByte(10)
            writeShort(definition.getOffsetY())
        }

        if (definition.getFade() != -1) {
            writeByte(11)
        }

        if (definition.getComparisonType() != -1) {
            writeByte(12)
            writeByte(definition.getComparisonType())
        }

        if (definition.getDamageYOfset() != 0) {
            writeByte(13)
            writeShort(definition.getDamageYOfset())
        }

        if (definition.getFade() != 0) {
            writeByte(14)
            writeShort(definition.getFade())
        }

        definition.writeTransforms(this, 17, 18)

        writeByte(0)
    }

}