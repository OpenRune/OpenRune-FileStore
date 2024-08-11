package dev.openrune.cache.filestore.definition.encoder

import dev.openrune.cache.filestore.buffer.Writer
import dev.openrune.cache.filestore.definition.ConfigEncoder
import dev.openrune.cache.filestore.definition.data.HealthBarType
import dev.openrune.cache.filestore.definition.data.HitSplatType

class HealthBarEncoder : ConfigEncoder<HealthBarType>() {

    override fun Writer.encode(definition: HealthBarType) {
        if (definition.getInt1() != 255) {
            writeByte(2)
            writeByte(definition.getInt1())
        }
        if (definition.getInt2() != 255) {
            writeByte(3)
            writeByte(definition.getInt2())
        }
        if (definition.getInt3() != -1) {
            writeByte(4)
        }
        if (definition.getInt4() != 70) {
            writeByte(5)
            writeShort(definition.getInt4())
        }

        if (definition.getFrontSpriteId() != -1) {
            writeByte(7)
            writeShort(definition.getFrontSpriteId())
        }
        if (definition.getBackSpriteId() != -1) {
            writeByte(8)
            writeShort(definition.getBackSpriteId())
        }
        if (definition.getInt3() != -1) {
            writeByte(11)
            writeShort(definition.getInt3())
        }
        if (definition.getWidth() != 30) {
            writeByte(14)
            writeByte(definition.getWidth())
        }

        if (definition.getWidthPadding() != 0) {
            writeByte(15)
            writeByte(definition.getWidthPadding())
        }

        writeByte(0)
    }

}