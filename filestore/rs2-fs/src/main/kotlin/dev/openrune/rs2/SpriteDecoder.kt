package dev.openrune.rs2

import dev.openrune.Rs2Cache
import dev.openrune.definition.type.SpriteType

class SpriteDecoder : Rs2Decoder<SpriteType> {

    override fun ids(cache: Rs2Cache): List<Int> = cache.spriteGroups()

    override fun read(cache: Rs2Cache, id: Int): SpriteType = cache.readSpriteType(group = id)

}
