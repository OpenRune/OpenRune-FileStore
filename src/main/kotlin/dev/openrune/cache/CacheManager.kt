package dev.openrune.cache

import dev.openrune.cache.filestore.Cache
import dev.openrune.cache.filestore.definition.data.*
import dev.openrune.cache.filestore.definition.decoder.*
import java.nio.file.Path

object CacheManager {


    private lateinit var cache: Cache
    private lateinit var npcs: Array<NPCDefinition>
    private lateinit var objects: Array<ObjectDefinition>
    private lateinit var items: Array<ItemDefinition>
    private lateinit var varbit: Array<VarBitDefinition>
    private lateinit var varps: Array<VarpDefinition>
    private lateinit var anim: Array<AnimDefinition>
    private lateinit var enum: Array<EnumDefinition>
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
    }

    fun getNpcs() = npcs
    fun getObjects() = objects
    fun getItems() = items

    fun npc(id: Int): NPCDefinition = npcs[id]
    fun npcCount(): Int = npcs.size

    fun objects(id: Int): ObjectDefinition = objects[id]
    fun objectCount(): Int = objects.size

    fun item(id: Int): ItemDefinition = items[id]
    fun itemCount(): Int = items.size

    fun varbit(id: Int): VarBitDefinition = varbit[id]
    fun varbitCount(): Int = varbit.size

    fun varp(id: Int): VarpDefinition = varps[id]
    fun varpCount(): Int = varps.size

    fun anim(id: Int): AnimDefinition = anim[id]

    fun enum(id: Int): EnumDefinition = enum[id]

    fun revisionIsOrAfter(rev : Int) = rev <= cacheRevision
    fun revisionIsOrBefore(rev : Int) = rev >= cacheRevision
}
