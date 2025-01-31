package dev.openrune.codec

import dev.openrune.cache.filestore.buffer.Reader
import dev.openrune.cache.filestore.buffer.Writer
import dev.openrune.cache.filestore.definition.DefinitionCodec
import dev.openrune.cache.filestore.definition.data.StructType

class StructCodec : DefinitionCodec<StructType> {
    override fun StructType.read(opcode: Int, buffer: Reader) {
        if (opcode == 249) {
            readParameters(buffer)
        }
    }

    override fun Writer.encode(definition: StructType) {
        definition.writeParameters(this)

        writeByte(0)
    }

    override fun createDefinition() = StructType()
}