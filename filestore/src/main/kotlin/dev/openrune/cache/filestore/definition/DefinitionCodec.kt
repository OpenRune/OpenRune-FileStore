package dev.openrune.cache.filestore.definition

import dev.openrune.cache.filestore.buffer.BufferReader
import dev.openrune.cache.filestore.buffer.Reader
import dev.openrune.cache.filestore.buffer.Writer

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
        val reader = BufferReader(data)
        val definition = createDefinition()
        readLoop(definition, reader)
        return definition
    }
}