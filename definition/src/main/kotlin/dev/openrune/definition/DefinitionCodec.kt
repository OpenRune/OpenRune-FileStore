package dev.openrune.definition

import io.netty.buffer.ByteBuf
import dev.openrune.buffer.Writer
import io.netty.buffer.Unpooled

interface DefinitionCodec<T : Definition> {

    fun readLoop(definition: T, buffer: ByteBuf) {
        while (true) {
            val opcode = buffer.readUnsignedByte().toInt()
            if (opcode == 0) {
                break
            }
            definition.read(opcode, buffer)
        }
    }

    fun T.read(opcode: Int, buffer: ByteBuf)
    fun Writer.encode(definition: T)

    fun createDefinition(): T

    fun loadData(id: Int, data: ByteArray): T {
        val reader = Unpooled.wrappedBuffer(data)
        val definition = createDefinition()
        readLoop(definition, reader)
        return definition
    }
}

fun revisionIsOrAfter(cacheRevision : Int, rev: Int) = rev <= cacheRevision
fun revisionIsOrBefore(cacheRevision : Int,rev: Int) = rev >= cacheRevision