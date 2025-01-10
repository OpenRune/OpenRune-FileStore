package dev.openrune.cache.filestore

import dev.openrune.cache.filestore.definition.data.NpcType

abstract class GameDataSource {

    open val npcs: MutableMap<Int, NpcType> = mutableMapOf()

    open var npcOffset: Int = 0
    open var objectOffset: Int = 0
    open var itemOffset: Int = 0
    open var varbitOffset: Int = 0
    open var varpOffset: Int = 0
    open var animOffset: Int = 0
    open var enumOffset: Int = 0
    open var healthBarOffset: Int = 0
    open var hitsplatOffset: Int = 0
    open var structOffset: Int = 0

    abstract fun init()

}