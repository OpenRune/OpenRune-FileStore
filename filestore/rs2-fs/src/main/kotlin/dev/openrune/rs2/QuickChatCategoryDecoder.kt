package dev.openrune.rs2

import dev.openrune.Rs2Cache
import dev.openrune.definition.type.QuickChatCategoryType

class QuickChatCategoryDecoder : Rs2Decoder<QuickChatCategoryType> {

    override fun ids(cache: Rs2Cache): List<Int> = cache.quickChatCategoryIds()

    override fun read(cache: Rs2Cache, id: Int): QuickChatCategoryType = cache.readQuickChatCategory(id)

}
