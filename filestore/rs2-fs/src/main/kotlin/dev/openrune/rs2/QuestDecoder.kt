package dev.openrune.rs2

import dev.openrune.Rs2Cache
import dev.openrune.definition.type.Rs2QuestType

class QuestDecoder : Rs2Decoder<Rs2QuestType> {

    override fun ids(cache: Rs2Cache): List<Int> = cache.questIds()

    override fun read(cache: Rs2Cache, id: Int): Rs2QuestType = cache.readQuest(id)

}
