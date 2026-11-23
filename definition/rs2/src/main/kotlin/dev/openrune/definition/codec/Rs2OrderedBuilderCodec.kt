package dev.openrune.definition.codec

import dev.openrune.definition.BuilderDefinitionCodec
import dev.openrune.definition.Definition
import dev.openrune.definition.Rs2OpcodeOrdered
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled

interface Rs2OrderedBuilderCodec<T, B> : BuilderDefinitionCodec<T, B> where T : Definition, T : Rs2OpcodeOrdered {

    fun ByteBuf.writePayload(opcode: Int, occurrence: Int, definition: T)

    override fun loadData(id: Int, data: ByteBuf?): T {
        val builder = builder(id)
        val order = if (data != null && data.readableBytes() > 0) {
            try {
                readOpcodeOrder(data) { opcode -> builder.read(opcode, data) }
            } catch (e: Exception) {
                throw IllegalStateException("Unable to decode ${createDefinition().javaClass.simpleName} [$id]", e)
            }
        } else {
            emptyList()
        }
        val definition = build(builder)
        definition.opcodeOrder += order
        return definition
    }

    override fun loadData(id: Int, data: ByteArray?): T =
        loadData(id, data?.takeIf { it.isNotEmpty() }?.let { Unpooled.wrappedBuffer(it) })

    override fun ByteBuf.encode(definition: T) {
        replayOpcodes(definition.opcodeOrder) { opcode, occurrence ->
            writePayload(opcode, occurrence, definition)
        }
    }
}
