package dev.openrune.cache

import dev.openrune.definition.type.*

abstract class CacheStore() {

    open var cacheRevision = -1

    open val npcs: MutableMap<Int, NpcType> = mutableMapOf()
    open val  objects: MutableMap<Int, dev.openrune.definition.type.ObjectType> = mutableMapOf()
    open val  items: MutableMap<Int, ItemType> = mutableMapOf()
    open val  varbits: MutableMap<Int, VarBitType> = mutableMapOf()
    open val  varps: MutableMap<Int, VarpType> = mutableMapOf()
    open val  anims: MutableMap<Int, SequenceType> = mutableMapOf()
    open val  enums: MutableMap<Int, EnumType> = mutableMapOf()
    open val  healthBars: MutableMap<Int, HealthBarType> = mutableMapOf()
    open val  hitsplats: MutableMap<Int, HitSplatType> = mutableMapOf()
    open val  structs: MutableMap<Int, StructType> = mutableMapOf()
    open val  dbrows: MutableMap<Int, DBRowType> = mutableMapOf()
    open val  dbtables: MutableMap<Int, DBTableType> = mutableMapOf()


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
    open var dbrowOffset: Int = 0
    open var dbtableOffset: Int = 0

    abstract fun init()

}