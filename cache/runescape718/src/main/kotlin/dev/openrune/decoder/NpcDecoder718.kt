package dev.openrune.decoder

import dev.openrune.Index.NPCS
import dev.openrune.cache.filestore.buffer.Reader
import dev.openrune.cache.filestore.definition.DefinitionCodec
import dev.openrune.cache.filestore.definition.DefinitionDecoder
import dev.openrune.cache.filestore.definition.data.NpcType
import dev.openrune.codec.NpcCodec718

class NpcDecoder718 : DefinitionDecoder<NpcType>(NPCS) {

    override fun getFile(id: Int) = id and 0x7f
    override fun createDefinition(): NpcType = NpcType()
    override fun getArchive(id: Int) = id ushr 7
    override fun isRS2() = true

    private val codec: DefinitionCodec<NpcType> = NpcCodec718()

    override fun NpcType.read(opcode: Int, buffer: Reader) {
        codec.run {
            this@read.read(opcode, buffer)
        }
    }
}