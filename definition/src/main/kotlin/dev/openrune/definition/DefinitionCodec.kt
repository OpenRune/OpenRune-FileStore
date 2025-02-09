package dev.openrune.definition

import dev.openrune.buffer.Reader
import dev.openrune.buffer.Writer

interface DefinitionCodec<T : Definition> {

    fun readLoop(definition: T, buffer: Reader) {
        while (true) {
            val opcode = buffer.readUnsignedByte()
            if (opcode == 0) {
                break
            }
            definition.read(opcode, buffer)
        }
    }

    fun T.read(opcode: Int, buffer: Reader)
    fun Writer.encode(definition: T)

    fun createDefinition(): T

    fun loadData(id: Int, data: ByteArray): T {
        val reader = Reader(data)
        val definition = createDefinition()
        readLoop(definition, reader)
        return definition
    }
}

fun revisionIsOrAfter(cacheRevision : Int, rev: Int) = rev <= cacheRevision
fun revisionIsOrBefore(cacheRevision : Int,rev: Int) = rev >= cacheRevision