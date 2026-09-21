package dev.openrune.definition.codec

import dev.openrune.definition.DefinitionCodec
import dev.openrune.definition.type.QuickChatCategoryRef
import dev.openrune.definition.type.QuickChatCategoryType
import dev.openrune.definition.type.QuickChatPhraseRef
import dev.openrune.definition.util.readStringCP
import dev.openrune.definition.util.writeStringCP
import io.netty.buffer.ByteBuf

/** Ported from [zwyz/rs3-cache](https://github.com/zwyz/rs3-cache)'s `QuickChatCatUnpacker.java`. */
class QuickChatCategoryCodec : DefinitionCodec<QuickChatCategoryType> {

    override fun QuickChatCategoryType.read(opcode: Int, buffer: ByteBuf) {
        when (opcode) {
            1 -> description = buffer.readStringCP()
            2 -> subCategories = List(buffer.readUnsignedByte().toInt()) {
                QuickChatCategoryRef(buffer.readUnsignedShort(), buffer.readByte().toInt())
            }
            3 -> phrases = List(buffer.readUnsignedByte().toInt()) {
                QuickChatPhraseRef(buffer.readUnsignedShort(), buffer.readByte().toInt())
            }
            4 -> searchable = true
        }
    }

    override fun ByteBuf.encode(definition: QuickChatCategoryType) {
        definition.description?.let {
            writeByte(1)
            writeStringCP(it)
        }
        if (definition.subCategories.isNotEmpty()) {
            writeByte(2)
            writeByte(definition.subCategories.size)
            definition.subCategories.forEach {
                writeShort(it.id)
                writeByte(it.priority)
            }
        }
        if (definition.phrases.isNotEmpty()) {
            writeByte(3)
            writeByte(definition.phrases.size)
            definition.phrases.forEach {
                writeShort(it.id)
                writeByte(it.priority)
            }
        }
        if (definition.searchable) writeByte(4)
        writeByte(0)
    }

    override fun createDefinition() = QuickChatCategoryType(0)
}
