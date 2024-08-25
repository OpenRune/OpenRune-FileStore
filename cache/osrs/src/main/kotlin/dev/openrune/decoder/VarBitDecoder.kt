package dev.openrune.decoder

import dev.openrune.cache.VARBIT
import dev.openrune.cache.filestore.definition.DefinitionDecoder
import dev.openrune.cache.filestore.buffer.Reader
import dev.openrune.cache.filestore.definition.data.VarBitType

class VarBitDecoder : DefinitionDecoder<VarBitType>(VARBIT) {

    override fun create(size: Int) = Array(size) { VarBitType(it) }

    override fun getFile(id: Int) = id

    override fun getArchive(id: Int) = 2

    override fun VarBitType.read(opcode: Int, buffer: Reader) {
        if (opcode == 1) {
            varp = buffer.readShort()
            startBit = buffer.readUnsignedByte()
            endBit = buffer.readUnsignedByte()
        }
    }
}