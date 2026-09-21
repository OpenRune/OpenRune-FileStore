package dev.openrune.rs2

import dev.openrune.Rs2Cache
import dev.openrune.definition.type.MapSceneIconType

class MapSceneIconDecoder : Rs2Decoder<MapSceneIconType> {

    override fun ids(cache: Rs2Cache): List<Int> = cache.mapSceneIconIds()

    override fun read(cache: Rs2Cache, id: Int): MapSceneIconType = cache.readMapSceneIcon(id)

}
