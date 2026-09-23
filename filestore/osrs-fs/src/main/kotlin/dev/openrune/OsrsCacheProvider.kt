package dev.openrune

import com.github.michaelbull.logging.InlineLogger
import dev.openrune.cache.*
import dev.openrune.cache.DenseIntMap
import dev.openrune.filesystem.Cache
import dev.openrune.cache.filestore.definition.ConfigDefinitionDecoder
import dev.openrune.cache.filestore.definition.DefinitionDecoder
import dev.openrune.cache.filestore.definition.DefinitionTransform
import dev.openrune.definition.type.*
import dev.openrune.definition.codec.*
import readCacheRevision
import java.nio.BufferUnderflowException

class OsrsCacheProvider(private val cache : Cache, override var cacheRevision : Int = readCacheRevision(cache)) : CacheStore() {

    private val logger = InlineLogger()

    override val npcs: MutableMap<Int, NpcType> = DenseIntMap()
    override val objects: MutableMap<Int, ObjectType> = DenseIntMap()
    override val items: MutableMap<Int, ItemType> = DenseIntMap()
    override val varbits: MutableMap<Int, VarBitType> = DenseIntMap()
    override val varps: MutableMap<Int, VarpType> = DenseIntMap()
    override val anims: MutableMap<Int, SequenceType> = DenseIntMap()
    override val enums: MutableMap<Int, EnumType> = DenseIntMap()
    override val healthBars: MutableMap<Int, HealthBarType> = DenseIntMap()
    override val hitsplats: MutableMap<Int, HitSplatType> = DenseIntMap()
    override val structs: MutableMap<Int, StructType> = DenseIntMap()
    override val dbrows: MutableMap<Int, DBRowType> = DenseIntMap()
    override val dbtables: MutableMap<Int, DBTableType> = DenseIntMap()

    override fun init() {
        try {
            logger.debug { "Cache loaded (revision $cacheRevision)" }
            ObjectDecoder(cacheRevision).load(cache, objects)
            NPCDecoder(cacheRevision).load(cache, npcs)
            ItemDecoder(cacheRevision).load(cache, items)
            VarBitDecoder().load(cache, varbits)
            VarDecoder().load(cache, varps)
            SequenceDecoder(cacheRevision).load(cache, anims)
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

    class AreaDecoder : ConfigDefinitionDecoder<MapElementType>(MapElementCodec(), MAP_ELEMENT)
    class DBRowDecoder : ConfigDefinitionDecoder<DBRowType>(DBRowCodec(), DBROW)
    class DBTableDecoder : ConfigDefinitionDecoder<DBTableType>(DBTableCodec(), DBTABLE)
    class EnumDecoder : ConfigDefinitionDecoder<EnumType>(EnumCodec(), ENUM)
    class HealthBarDecoder : ConfigDefinitionDecoder<HealthBarType>(HealthBarCodec(), HEALTHBAR)
    class AmbienceDecoder : ConfigDefinitionDecoder<AmbienceType>(AmbienceCodec(), AMBIENCE)
    class HitSplatDecoder : ConfigDefinitionDecoder<HitSplatType>(HitSplatCodec(), HITSPLAT)
    class ItemDecoder(cacheRevision: Int) : ConfigDefinitionDecoder<ItemType>(ItemCodec(cacheRevision), ITEM)
    class NPCDecoder(cacheRevision: Int) : ConfigDefinitionDecoder<NpcType>(NPCCodec(cacheRevision), NPC)
    class ObjectDecoder(cacheRevision: Int) : ConfigDefinitionDecoder<ObjectType>(ObjectCodec(cacheRevision), OBJECT)

    class OverlayDecoder : ConfigDefinitionDecoder<OverlayType>(OverlayCodec(), OVERLAY)
    class ParamDecoder(cacheRevision: Int) : ConfigDefinitionDecoder<ParamType>(ParamCodec(cacheRevision), PARAMS)
    class SequenceDecoder(cacheRevision: Int) : ConfigDefinitionDecoder<SequenceType>(SequenceCodec(cacheRevision), SEQUENCE)
    class StructDecoder : ConfigDefinitionDecoder<StructType>(StructCodec(), STRUCT)
    class UnderlayDecoder : ConfigDefinitionDecoder<UnderlayType>(UnderlayCodec(), UNDERLAY)
    class VarBitDecoder : ConfigDefinitionDecoder<VarBitType>(VarBitCodec(), VARBIT)
    class VarDecoder : ConfigDefinitionDecoder<VarpType>(VarCodec(), VARPLAYER)
    class IdentityKitDecoder(cacheRevision: Int) : ConfigDefinitionDecoder<IdentityKitType>(IdentityKitCodec(cacheRevision), IDENTKIT)
    class InventoryDecoder : ConfigDefinitionDecoder<InventoryType>(InventoryCodec(), INV)
    class SpotAnimDecoder(cacheRevision: Int) : ConfigDefinitionDecoder<SpotAnimType>(SpotAnimCodec(cacheRevision), SPOTANIM)
    class VarClientDecoder : ConfigDefinitionDecoder<VarClientType>(VarClientCodec(), VARCLIENT)
    class WorldEntityDecoder : ConfigDefinitionDecoder<WorldEntityType>(WorldEntityCodec(), WORLD_ENTITY)
    class BugTemplateDecoder : ConfigDefinitionDecoder<BugTemplateType>(BugTemplateCodec(), BUGTEMPLATE)
    class StringVectorDecoder : ConfigDefinitionDecoder<StringVectorType>(StringVectorCodec(), STRINGVECTOR)
    class VarClanDecoder() : ConfigDefinitionDecoder<VarClanType>(VarClanCodec(), VAR_CLAN)
    class VarClanSettingDecoder() : ConfigDefinitionDecoder<VarClanSettingsType>(VarClanSettingCodec(), VAR_CLAN_SETTINGS)

    class TextureDecoder(cacheRevision: Int) : DefinitionDecoder<TextureType>(TEXTURES, TextureCodec(cacheRevision)) {
        override fun getArchive(id: Int) = 0
        override fun getFile(id: Int) = id
    }

    class WorldMapAreasDecoder(cacheRevision: Int) : DefinitionDecoder<WorldMapAreaType>(WORLDMAPAREAS, WorldMapAreaCodec(cacheRevision)) {
        override fun getArchive(id: Int) = 0
        override fun getFile(id: Int) = id
    }

}