package dev.openrune.rs2

import com.github.michaelbull.logging.InlineLogger
import dev.openrune.Rs2Cache
import dev.openrune.definition.type.BasType
import dev.openrune.definition.type.BillboardType
import dev.openrune.definition.type.EnumType
import dev.openrune.definition.type.FloorOverlayType
import dev.openrune.definition.type.FloorUnderlayType
import dev.openrune.definition.type.HeadbarType
import dev.openrune.definition.type.HitmarkType
import dev.openrune.definition.type.LightType
import dev.openrune.definition.type.ParamType
import dev.openrune.definition.type.ParticleEffectorType
import dev.openrune.definition.type.ParticleEmitterType
import dev.openrune.definition.type.Rs2DbRowType
import dev.openrune.definition.type.Rs2DbTableType
import dev.openrune.definition.type.Rs2IdentityKitType
import dev.openrune.definition.type.Rs2NpcType
import dev.openrune.definition.type.Rs2ObjType
import dev.openrune.definition.type.Rs2SequenceType
import dev.openrune.definition.type.Rs2SpotAnimType
import dev.openrune.definition.type.Rs2VarBitType
import dev.openrune.definition.type.StructType
import dev.openrune.definition.type.VarClientType
import dev.openrune.definition.type.WaterType
import dev.openrune.definition.type.WorldAreaType

class Rs2CacheProvider(private val cache: Rs2Cache) {

    private val logger = InlineLogger()

    val npcs: MutableMap<Int, Rs2NpcType> = HashMap()
    val objs: MutableMap<Int, Rs2ObjType> = HashMap()
    val anims: MutableMap<Int, Rs2SequenceType> = HashMap()
    val spotAnims: MutableMap<Int, Rs2SpotAnimType> = HashMap()
    val identityKits: MutableMap<Int, Rs2IdentityKitType> = HashMap()
    val enums: MutableMap<Int, EnumType> = HashMap()
    val structs: MutableMap<Int, StructType> = HashMap()
    val params: MutableMap<Int, ParamType> = HashMap()
    val varClients: MutableMap<Int, VarClientType> = HashMap()
    val varBits: MutableMap<Int, Rs2VarBitType> = HashMap()
    val dbRows: MutableMap<Int, Rs2DbRowType> = HashMap()
    val dbTables: MutableMap<Int, Rs2DbTableType> = HashMap()
    val floorOverlays: MutableMap<Int, FloorOverlayType> = HashMap()
    val floorUnderlays: MutableMap<Int, FloorUnderlayType> = HashMap()
    val headbars: MutableMap<Int, HeadbarType> = HashMap()
    val hitmarks: MutableMap<Int, HitmarkType> = HashMap()
    val lights: MutableMap<Int, LightType> = HashMap()
    val bas: MutableMap<Int, BasType> = HashMap()
    val waters: MutableMap<Int, WaterType> = HashMap()
    val worldAreas: MutableMap<Int, WorldAreaType> = HashMap()
    val particleEmitters: MutableMap<Int, ParticleEmitterType> = HashMap()
    val particleEffectors: MutableMap<Int, ParticleEffectorType> = HashMap()
    val billboards: MutableMap<Int, BillboardType> = HashMap()

    fun init() {
        logger.debug { "RS3 cache loaded (build ${cache.build})" }
        Rs2NpcDecoder().load(cache, npcs)
        Rs2ObjDecoder().load(cache, objs)
        Rs2SequenceDecoder().load(cache, anims)
        Rs2SpotAnimDecoder().load(cache, spotAnims)
        Rs2IdentityKitDecoder().load(cache, identityKits)
        Rs2EnumDecoder().load(cache, enums)
        Rs2StructDecoder().load(cache, structs)
        Rs2ParamDecoder().load(cache, params)
        Rs2VarClientDecoder().load(cache, varClients)
        Rs2VarBitDecoder().load(cache, varBits)
        Rs2DbRowDecoder().load(cache, dbRows)
        Rs2DbTableDecoder().load(cache, dbTables)
        Rs2FloorOverlayDecoder().load(cache, floorOverlays)
        Rs2FloorUnderlayDecoder().load(cache, floorUnderlays)
        Rs2HeadbarDecoder().load(cache, headbars)
        Rs2HitmarkDecoder().load(cache, hitmarks)
        Rs2LightDecoder().load(cache, lights)
        Rs2BasDecoder().load(cache, bas)
        Rs2WaterDecoder().load(cache, waters)
        Rs2WorldAreaDecoder().load(cache, worldAreas)
        Rs2ParticleEmitterDecoder().load(cache, particleEmitters)
        Rs2ParticleEffectorDecoder().load(cache, particleEffectors)
        Rs2BillboardDecoder().load(cache, billboards)
    }
}
