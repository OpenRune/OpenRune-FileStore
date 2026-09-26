package dev.openrune.rs2

import dev.openrune.Rs2Cache
import dev.openrune.definition.type.Rs2AchievementType

class AchievementDecoder : Rs2Decoder<Rs2AchievementType> {

    override fun ids(cache: Rs2Cache): List<Int> = cache.achievementIds()

    override fun read(cache: Rs2Cache, id: Int): Rs2AchievementType = cache.readAchievement(id)

}
