package dev.openrune

import dev.openrune.cache.CONFIGS
import dev.openrune.cache.filestore.buffer.Reader
import dev.openrune.cache.filestore.definition.Definition
import dev.openrune.cache.filestore.definition.DefinitionCodec
import dev.openrune.cache.filestore.definition.DefinitionDecoder
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap

abstract class DefinitionDecoderOSRS<T : Definition>(
    val codec: DefinitionCodec<T>,
    private val archive: Int,
    private val factory: () -> T
) : DefinitionDecoder<T>(CONFIGS) {

    override fun getArchive(id: Int) = archive

    override fun createDefinition(): T = factory()

    override fun getFile(id: Int) = id

    override fun T.read(opcode: Int, buffer: Reader) {
        codec.run {
            this@read.read(opcode, buffer)
        }
    }

}
