package dev.openrune.cache

import dev.openrune.cache.filestore.Cache
import dev.openrune.cache.filestore.definition.data.*
import dev.openrune.cache.filestore.definition.decoder.*
import java.nio.file.Path

object CacheManager {

    lateinit var cache: Cache
    private lateinit var npcs: Array<NpcType>
    private lateinit var objects: Array<ObjectType>
    private lateinit var items: Array<ItemType>
    private lateinit var varbit: Array<VarBitType>
    private lateinit var varps: Array<VarpType>
    private lateinit var anim: Array<AnimType>
    private lateinit var enum: Array<EnumType>
    private lateinit var health: Array<HealthBarType>
    private lateinit var hitsplats: Array<HitSplatType>
    private lateinit var struct: Array<StructType>
    private var cacheRevision = -1


    fun init(cachePath: Path, cacheRevision : Int) {
        this.cacheRevision = cacheRevision;
        cache = Cache.load(cachePath, false)
        npcs = NPCDecoder().load(cache)
        objects = ObjectDecoder().load(cache)
        items = ItemDecoder().load(cache)
        varbit = VarBitDecoder().load(cache)
        varps = VarDecoder().load(cache)
        anim = AnimDecoder().load(cache)
        enum = EnumDecoder().load(cache)
        health = HealthBarDecoder().load(cache)
        hitsplats = HitSplatDecoder().load(cache)
        struct = StructDecoder().load(cache)

    }

    fun getNpcs() = npcs
    fun getObjects() = objects
    fun getItems() = items

    fun getHitsplats() = hitsplats

    fun getStructs() = struct

    fun health(id: Int): HealthBarType = health[id]

    fun healthCount(): Int = health.size

    fun npc(id: Int): NpcType = npcs[id]
    fun npcCount(): Int = npcs.size

    fun objects(id: Int): ObjectType = objects[id]
    fun objectCount(): Int = objects.size

    fun item(id: Int): ItemType = items[id]
    fun itemCount(): Int = items.size

    fun varbit(id: Int): VarBitType = varbit[id]
    fun varbitCount(): Int = varbit.size

    fun varp(id: Int): VarpType = varps[id]
    fun varpCount(): Int = varps.size

    fun anim(id: Int): AnimType = anim[id]

    fun enum(id: Int): EnumType = enum[id]

    fun hitsplat(id: Int): HitSplatType = hitsplats[id]
    fun hitsplatCount(): Int = hitsplats.size

    fun struct(id: Int): StructType = struct[id]
    fun structCount(): Int = struct.size

    fun revisionIsOrAfter(rev : Int) = rev <= cacheRevision
    fun revisionIsOrBefore(rev : Int) = rev >= cacheRevision
}
