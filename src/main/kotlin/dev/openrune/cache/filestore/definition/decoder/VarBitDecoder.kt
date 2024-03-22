package dev.openrune.cache.filestore.definition.decoder

import dev.openrune.cache.VARBIT
import dev.openrune.cache.filestore.definition.DefinitionDecoder
import dev.openrune.cache.filestore.buffer.Reader
import dev.openrune.cache.filestore.definition.data.VarBitDefinition

class VarBitDecoder : DefinitionDecoder<VarBitDefinition>(VARBIT) {

    override fun create(size: Int) = Array(size) { VarBitDefinition(it) }

    override fun getFile(id: Int) = id

    override fun VarBitDefinition.read(opcode: Int, buffer: Reader) {
        if (opcode == 1) {
            varp = buffer.readShort()
            startBit = buffer.readUnsignedByte()
            endBit = buffer.readUnsignedByte()
        }
    }
}