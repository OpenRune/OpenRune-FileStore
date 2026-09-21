package dev.openrune.rs2

import dev.openrune.Rs2Cache
import dev.openrune.definition.type.Rs2MapElementType

class MapElementDecoder : Rs2Decoder<Rs2MapElementType> {

    override fun ids(cache: Rs2Cache): List<Int> = cache.mapElementIds()

    override fun read(cache: Rs2Cache, id: Int): Rs2MapElementType = cache.readMapElement(id)

}
