package dev.openrune

import dev.openrune.OsrsCacheProvider.Companion.CACHE_REVISION
import dev.openrune.cache.*
import dev.openrune.cache.filestore.definition.Definition
import dev.openrune.cache.filestore.definition.DefinitionCodec
import dev.openrune.cache.filestore.definition.DefinitionDecoder
import dev.openrune.cache.filestore.definition.data.*
import dev.openrune.codec.*

sealed class DefinitionDecoderOSRS<T : Definition>(
    codec: DefinitionCodec<T>,
    private val archive: Int,
) : DefinitionDecoder<T>(CONFIGS, codec) {

    override fun getArchive(id: Int) = archive

    override fun getFile(id: Int) = id

    class AreaDecoder : DefinitionDecoderOSRS<AreaType>(AreaCodec(), AREA)
    class DBRowDecoder : DefinitionDecoderOSRS<DBRowType>(DBRowCodec(), DBROW)
    class DBTableDecoder : DefinitionDecoderOSRS<DBTableType>(DBTableCodec(), DBTABLE)
    class EnumDecoder : DefinitionDecoderOSRS<EnumType>(EnumCodec(), ENUM)
    class HealthBarDecoder : DefinitionDecoderOSRS<HealthBarType>(HealthBarCodec(), HEALTHBAR)
    class HitSplatDecoder : DefinitionDecoderOSRS<HitSplatType>(HitSplatCodec(), HITSPLAT)
    class ItemDecoder : DefinitionDecoderOSRS<ItemType>(ItemCodec(), ITEM)
    class NPCDecoder : DefinitionDecoderOSRS<NpcType>(NPCCodec(CACHE_REVISION), NPC)
    class ObjectDecoder : DefinitionDecoderOSRS<ObjectType>(ObjectCodec(CACHE_REVISION), OBJECT)
    class OverlayDecoder : DefinitionDecoderOSRS<OverlayType>(OverlayCodec(), OVERLAY)
    class ParamDecoder : DefinitionDecoderOSRS<ParamType>(ParamCodec(), PARAMS)
    class SequenceDecoder : DefinitionDecoderOSRS<SequenceType>(SequenceCodec(CACHE_REVISION), SEQUENCE)
    class StructDecoder : DefinitionDecoderOSRS<StructType>(StructCodec(), STRUCT)
    class UnderlayDecoder : DefinitionDecoderOSRS<UnderlayType>(UnderlayCodec(), UNDERLAY)
    class VarBitDecoder : DefinitionDecoderOSRS<VarBitType>(VarBitCodec(), VARBIT)
    class VarDecoder : DefinitionDecoderOSRS<VarpType>(VarCodec(), VARPLAYER)
}
