package dev.openrune.decoder

import dev.openrune.Index.ITEMS
import dev.openrune.cache.filestore.definition.DefinitionDecoder
import dev.openrune.cache.filestore.buffer.Reader
import dev.openrune.cache.filestore.definition.DefinitionCodec
import dev.openrune.cache.filestore.definition.data.ItemType
import dev.openrune.codec.ItemCodec718

class ItemDecoder718 : DefinitionDecoder<ItemType>(ITEMS) {

    override fun getFile(id: Int) = id and 0xff
    override fun createDefinition(): ItemType = ItemType()
    override fun getArchive(id: Int) = id ushr 8
    override fun isRS2() = true

    private val codec: DefinitionCodec<ItemType> = ItemCodec718()

    override fun ItemType.read(opcode: Int, buffer: Reader) {
        codec.run {
            this@read.read(opcode, buffer)
        }
    }
}