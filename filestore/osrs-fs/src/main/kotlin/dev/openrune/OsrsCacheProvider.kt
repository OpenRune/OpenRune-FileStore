package dev.openrune

import dev.openrune.cache.*
import dev.openrune.filesystem.Cache
import dev.openrune.cache.filestore.definition.ConfigDefinitionDecoder
import dev.openrune.cache.filestore.definition.DefinitionDecoder
import dev.openrune.definition.type.*
import dev.openrune.definition.codec.*
import io.github.oshai.kotlinlogging.KotlinLogging
import java.nio.BufferUnderflowException

class OsrsCacheProvider(private val cache : Cache, override var cacheRevision : Int = -1) : CacheStore() {

    private val logger = KotlinLogging.logger {}

    init {
        CACHE_REVISION = cacheRevision
    }

    companion object {
        var CACHE_REVISION = -1
    }

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
    override val dbrows: MutableMap<Int, DBRowType> = mutableMapOf()
    override val dbtables: MutableMap<Int, DBTableType> = mutableMapOf()

    override fun init() {
        try {
            // Use the decoders to load the definitions directly into the maps
            ObjectDecoder().load(cache, objects)
            NPCDecoder().load(cache, npcs)
            ItemDecoder().load(cache, items)
            VarBitDecoder().load(cache, varbits)
            VarDecoder().load(cache, varps)
            SequenceDecoder().load(cache, anims)
            EnumDecoder().load(cache, enums)
            HealthBarDecoder().load(cache, healthBars)
            HitSplatDecoder().load(cache, hitsplats)
            StructDecoder().load(cache, structs)
            DBRowDecoder().load(cache, dbrows)
            DBTableDecoder().load(cache, dbtables)
        } catch (e: BufferUnderflowException) {
            logger.error(e) { "Error reading definitions" }
            throw e
        }
    }

    fun findScriptId(name: String): Int {
        val cacheName = "[clientscript,$name]"
        return cache.archiveId(CLIENTSCRIPT, cacheName).also { id ->
            if (id == -1) println("Unable to find script: $cacheName")
        }
    }

    class AreaDecoder : ConfigDefinitionDecoder<AreaType>(AreaCodec(), AREA)
    class DBRowDecoder : ConfigDefinitionDecoder<DBRowType>(DBRowCodec(), DBROW)
    class DBTableDecoder : ConfigDefinitionDecoder<DBTableType>(DBTableCodec(), DBTABLE)
    class EnumDecoder : ConfigDefinitionDecoder<EnumType>(EnumCodec(), ENUM)
    class HealthBarDecoder : ConfigDefinitionDecoder<HealthBarType>(HealthBarCodec(), HEALTHBAR)
    class HitSplatDecoder : ConfigDefinitionDecoder<HitSplatType>(HitSplatCodec(), HITSPLAT)
    class ItemDecoder : ConfigDefinitionDecoder<ItemType>(ItemCodec(), ITEM)
    class NPCDecoder : ConfigDefinitionDecoder<NpcType>(NPCCodec(CACHE_REVISION), NPC)
    class ObjectDecoder : ConfigDefinitionDecoder<ObjectType>(ObjectCodec(CACHE_REVISION), OBJECT)
    class OverlayDecoder : ConfigDefinitionDecoder<OverlayType>(OverlayCodec(), OVERLAY)
    class ParamDecoder : ConfigDefinitionDecoder<ParamType>(ParamCodec(), PARAMS)
    class SequenceDecoder : ConfigDefinitionDecoder<SequenceType>(SequenceCodec(CACHE_REVISION), SEQUENCE)
    class StructDecoder : ConfigDefinitionDecoder<StructType>(StructCodec(), STRUCT)
    class UnderlayDecoder : ConfigDefinitionDecoder<UnderlayType>(UnderlayCodec(), UNDERLAY)
    class VarBitDecoder : ConfigDefinitionDecoder<VarBitType>(VarBitCodec(), VARBIT)
    class VarDecoder : ConfigDefinitionDecoder<VarpType>(VarCodec(), VARPLAYER)
    class IdentityKitDecoder : ConfigDefinitionDecoder<IdentityKitType>(IdentityKitCodec(), IDENTKIT)
    class InventoryDecoder : ConfigDefinitionDecoder<InventoryType>(InventoryCodec(), INV)
    class SpotAnimDecoder : ConfigDefinitionDecoder<SpotAnimType>(SpotAnimCodec(), SPOTANIM)
    class VarClientDecoder : ConfigDefinitionDecoder<VarClientType>(VarClientCodec(), VARCLIENT)

    class TextureDecoder : DefinitionDecoder<TextureType>(TEXTURES, TextureCodec()) {
        override fun getArchive(id: Int) = 0
        override fun getFile(id: Int) = id
    }
}