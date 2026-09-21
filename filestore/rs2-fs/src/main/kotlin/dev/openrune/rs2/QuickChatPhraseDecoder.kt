package dev.openrune.rs2

import dev.openrune.Rs2Cache
import dev.openrune.definition.type.QuickChatPhraseType

class QuickChatPhraseDecoder : Rs2Decoder<QuickChatPhraseType> {

    override fun ids(cache: Rs2Cache): List<Int> = cache.quickChatPhraseIds()

    override fun read(cache: Rs2Cache, id: Int): QuickChatPhraseType = cache.readQuickChatPhrase(id)

}
