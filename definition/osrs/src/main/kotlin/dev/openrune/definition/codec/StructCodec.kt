package dev.openrune.definition.codec

import dev.openrune.buffer.Reader
import dev.openrune.buffer.Writer
import dev.openrune.definition.DefinitionCodec
import dev.openrune.definition.type.StructType

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