package dev.openrune

import dev.openrune.OsrsCacheProvider.Companion.CACHE_REVISION
import dev.openrune.cache.*
import dev.openrune.cache.filestore.buffer.Reader
import dev.openrune.cache.filestore.definition.Definition
import dev.openrune.cache.filestore.definition.DefinitionCodec
import dev.openrune.cache.filestore.definition.DefinitionDecoder
import dev.openrune.cache.filestore.definition.data.*
import dev.openrune.codec.*

sealed class DefinitionDecoderOSRS<T : Definition>(
    codec: DefinitionCodec<T>,
    private val archive: Int,
    private val factory: () -> T,
) : DefinitionDecoder<T>(CONFIGS, codec) {

    override fun getArchive(id: Int) = archive

    override fun createDefinition(): T = factory()

    override fun getFile(id: Int) = id

    class AreaDecoder : DefinitionDecoderOSRS<AreaType>(AreaCodec(), AREA, ::AreaType)
    class DBRowDecoder : DefinitionDecoderOSRS<DBRowType>(DBRowCodec(), DBROW, ::DBRowType)
    class DBTableDecoder : DefinitionDecoderOSRS<DBTableType>(DBTableCodec(), DBTABLE, ::DBTableType)
    class EnumDecoder : DefinitionDecoderOSRS<EnumType>(EnumCodec(), ENUM, ::EnumType)
    class HealthBarDecoder : DefinitionDecoderOSRS<HealthBarType>(HealthBarCodec(), HEALTHBAR, ::HealthBarType)
    class HitSplatDecoder : DefinitionDecoderOSRS<HitSplatType>(HitSplatCodec(), HITSPLAT, ::HitSplatType)
    class ItemDecoder : DefinitionDecoderOSRS<ItemType>(ItemCodec(), ITEM, ::ItemType)
    class NPCDecoder : DefinitionDecoderOSRS<NpcType>(NPCCodec(CACHE_REVISION), NPC, ::NpcType)
    class ObjectDecoder : DefinitionDecoderOSRS<ObjectType>(ObjectCodec(CACHE_REVISION), OBJECT, ::ObjectType)
    class OverlayDecoder : DefinitionDecoderOSRS<OverlayType>(OverlayCodec(), OVERLAY, ::OverlayType)
    class ParamDecoder : DefinitionDecoderOSRS<ParamType>(ParamCodec(), PARAMS, ::ParamType)
    class SequenceDecoder : DefinitionDecoderOSRS<SequenceType>(SequenceCodec(CACHE_REVISION), SEQUENCE, ::SequenceType)
    class StructDecoder : DefinitionDecoderOSRS<StructType>(StructCodec(), STRUCT, ::StructType)
    class UnderlayDecoder : DefinitionDecoderOSRS<UnderlayType>(UnderlayCodec(), UNDERLAY, ::UnderlayType)
    class VarBitDecoder : DefinitionDecoderOSRS<VarBitType>(VarBitCodec(), VARBIT, ::VarBitType)
    class VarDecoder : DefinitionDecoderOSRS<VarpType>(VarCodec(), VARPLAYER, ::VarpType)
}
