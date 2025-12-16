package dev.openrune.definition.opcode

import dev.openrune.definition.Definition
import dev.openrune.definition.DefinitionCodec
import dev.openrune.definition.util.toArray
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled

abstract class OpcodeDefinitionCodec<T : Definition> : DefinitionCodec<T> {
    abstract val definitionCodec: OpcodeList<T>

    open fun T.createData() {

    }

    fun encodeToBuffer(definition: T): ByteArray {
        val writer = Unpooled.buffer(4096)
        with(this) { writer.encode(definition) }
        return writer.toArray()
    }

    override fun T.read(opcode: Int, buffer: ByteBuf) {
        val defOpcode = definitionCodec.getOpcode(opcode)
            ?: error("Unknown opcode $opcode for ${this::class.simpleName}")
        defOpcode.decode(opcode, buffer, this)
    }

    override fun ByteBuf.encode(definition: T) {
        definition.createData()
        for (opcodeDef in definitionCodec.registeredOpcodes) {
            if (opcodeDef.shouldEncode(definition)) {
                opcodeDef.encode(this, definition)
            }
        }
        writeByte(0)
    }

    abstract override fun createDefinition(): T
}