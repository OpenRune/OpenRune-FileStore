package dev.openrune.definition.codec

import dev.openrune.buffer.Reader
import dev.openrune.buffer.Writer
import dev.openrune.definition.DefinitionCodec
import dev.openrune.definition.type.SpotAnimType
import dev.openrune.definition.type.VarClientType
import dev.openrune.definition.type.VarpType

class VarClientCodec : DefinitionCodec<VarClientType> {
    override fun VarClientType.read(opcode: Int, buffer: Reader) {
        when(opcode) {
            2 -> persist = true
        }
    }

    override fun Writer.encode(definition: VarClientType) {
        if (definition.persist) {
            writeByte(2)
        }
        writeByte(0)
    }

    override fun createDefinition() = VarClientType()
}