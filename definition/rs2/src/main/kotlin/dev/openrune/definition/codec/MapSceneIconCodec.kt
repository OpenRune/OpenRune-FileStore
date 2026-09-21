package dev.openrune.definition.codec

import dev.openrune.definition.DefinitionCodec
import dev.openrune.definition.type.MapSceneIconType
import dev.openrune.definition.util.readNullableLargeSmartCorrect
import dev.openrune.definition.util.writeNullableLargeSmartCorrect
import io.netty.buffer.ByteBuf

/** Ported from [zwyz/rs3-cache](https://github.com/zwyz/rs3-cache)'s `MSIUnpacker.java`. */
class MapSceneIconCodec : DefinitionCodec<MapSceneIconType> {

    override fun MapSceneIconType.read(opcode: Int, buffer: ByteBuf) {
        when (opcode) {
            1 -> graphic = buffer.readNullableLargeSmartCorrect()
            2 -> tint = buffer.readUnsignedMedium()
            3 -> resize = true
            4 -> blankGraphic = true
            5 -> hideOnMinimap = true
        }
    }

    override fun ByteBuf.encode(definition: MapSceneIconType) {
        if (definition.blankGraphic) {
            writeByte(4)
        } else if (definition.graphic != null) {
            writeByte(1)
            writeNullableLargeSmartCorrect(definition.graphic)
        }
        if (definition.tint != 0) {
            writeByte(2)
            writeMedium(definition.tint)
        }
        if (definition.resize) writeByte(3)
        if (definition.hideOnMinimap) writeByte(5)
        writeByte(0)
    }

    override fun createDefinition() = MapSceneIconType(0)
}
