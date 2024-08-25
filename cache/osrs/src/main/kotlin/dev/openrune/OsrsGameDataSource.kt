package dev.openrune

import dev.openrune.cache.GameDataSource
import dev.openrune.cache.filestore.Cache
import dev.openrune.cache.filestore.definition.data.*
import dev.openrune.decoder.*

class OsrsGameDataSource(val cache : Cache, val cacheRevision : Int) : GameDataSource() {

    override val npcs: MutableMap<Int, NpcType> = mutableMapOf()
    override val objects: MutableMap<Int, ObjectType> = mutableMapOf()
    override val items: MutableMap<Int, ItemType> = mutableMapOf()
    override val varbits: MutableMap<Int, VarBitType> = mutableMapOf()
    override val varps: MutableMap<Int, VarpType> = mutableMapOf()
    override val anims: MutableMap<Int, SequenceType> = mutableMapOf()
    override val enums: MutableMap<Int, EnumType> = mutableMapOf()
    override val healthBars: MutableMap<Int, HealthBarType> = mutableMapOf()
    override val hitsplats: MutableMap<Int, HitSplatType> = mutableMapOf()
    override val structs: MutableMap<Int, StructType> = mutableMapOf()

    override fun init() {
        npcs.putAll(NPCDecoder().load(cache))
        objects.putAll(ObjectDecoder().load(cache))
        items.putAll(ItemDecoder().load(cache))
        varbits.putAll(VarBitDecoder().load(cache))
        varps.putAll(VarDecoder().load(cache))
        anims.putAll(SequenceDecoder().load(cache))
        enums.putAll(EnumDecoder().load(cache))
        healthBars.putAll(HealthBarDecoder().load(cache))
        hitsplats.putAll(HitSplatDecoder().load(cache))
        structs.putAll(StructDecoder().load(cache))
    }

}