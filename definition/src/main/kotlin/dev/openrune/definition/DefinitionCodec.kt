package dev.openrune.definition

import io.netty.buffer.ByteBuf
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
    fun ByteBuf.encode(definition: T)

    fun createDefinition(): T

    /**
     * Reads a new [Definition] of type [T] from the nullable [ByteBuf].
     * If this [ByteBuf] is null or does not have any readable bytes a new [T] is returned with an id of [id]
     * NOTE: When calling this method, ensure that the readerIndex is set to 0 and
     * that the ByteBuf is the raw definition data which has been extracted from the Container/Group within
     * the archive/index.
     */
    fun loadData(id: Int, data: ByteBuf?): T {
        val definition = createDefinition()
        definition.id = id
        if(data != null && data.readableBytes() > 0) {
            try {
                readLoop(definition, data)
            }catch (e: Exception) {
                error("Unable to decode ${definition.javaClass.simpleName} [$id]")
            }
        }
        return definition
    }

    fun loadData(id: Int, data: ByteArray?): T {
        val definition = createDefinition()
        definition.id = id
        if(data != null && data.isNotEmpty()) {
            val reader = Unpooled.wrappedBuffer(data)
            try {
                readLoop(definition, reader)
            }catch (e: Exception) {
                //error("Unable to decode ${definition.javaClass.simpleName} [$id]")
            }
        }
        return definition
    }
}

fun revisionIsOrAfter(cacheRevision : Int, rev: Int) = rev <= cacheRevision
fun revisionIsOrBefore(cacheRevision : Int,rev: Int) = rev >= cacheRevision