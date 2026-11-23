package dev.openrune.rs2

import dev.openrune.Rs2Cache
import dev.openrune.definition.Definition
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
import dev.openrune.definition.type.Rs2LocType
import dev.openrune.definition.type.Rs2NpcType
import dev.openrune.definition.type.Rs2ObjType
import dev.openrune.definition.type.Rs2SequenceType
import dev.openrune.definition.type.Rs2SpotAnimType
import dev.openrune.definition.type.Rs2VarBitType
import dev.openrune.definition.type.StructType
import dev.openrune.definition.type.VarClientType
import dev.openrune.definition.type.WaterType
import dev.openrune.definition.type.WorldAreaType

class Rs2CacheManager(val cache: Rs2Cache) {

    fun loc(id: Int): Rs2LocType? = read(locs, id)
    fun npc(id: Int): Rs2NpcType? = read(npcs, id)
    fun obj(id: Int): Rs2ObjType? = read(objs, id)
    fun seq(id: Int): Rs2SequenceType? = read(seqs, id)
    fun spotAnim(id: Int): Rs2SpotAnimType? = read(spotAnims, id)
    fun identityKit(id: Int): Rs2IdentityKitType? = read(identityKits, id)
    fun enum(id: Int): EnumType? = read(enums, id)
    fun struct(id: Int): StructType? = read(structs, id)
    fun param(id: Int): ParamType? = read(params, id)
    fun varClient(id: Int): VarClientType? = read(varClients, id)
    fun varBit(id: Int): Rs2VarBitType? = read(varBits, id)
    fun dbRow(id: Int): Rs2DbRowType? = read(dbRows, id)
    fun dbTable(id: Int): Rs2DbTableType? = read(dbTables, id)
    fun floorOverlay(id: Int): FloorOverlayType? = read(floorOverlays, id)
    fun floorUnderlay(id: Int): FloorUnderlayType? = read(floorUnderlays, id)
    fun headbar(id: Int): HeadbarType? = read(headbars, id)
    fun hitmark(id: Int): HitmarkType? = read(hitmarks, id)
    fun light(id: Int): LightType? = read(lights, id)
    fun bas(id: Int): BasType? = read(bases, id)
    fun water(id: Int): WaterType? = read(waters, id)
    fun worldArea(id: Int): WorldAreaType? = read(worldAreas, id)
    fun particleEmitter(id: Int): ParticleEmitterType? = read(particleEmitters, id)
    fun particleEffector(id: Int): ParticleEffectorType? = read(particleEffectors, id)
    fun billboard(id: Int): BillboardType? = read(billboards, id)

    fun locIds(): List<Int> = locs.ids(cache)
    fun npcIds(): List<Int> = npcs.ids(cache)
    fun objIds(): List<Int> = objs.ids(cache)
    fun seqIds(): List<Int> = seqs.ids(cache)
    fun spotAnimIds(): List<Int> = spotAnims.ids(cache)
    fun identityKitIds(): List<Int> = identityKits.ids(cache)
    fun enumIds(): List<Int> = enums.ids(cache)
    fun structIds(): List<Int> = structs.ids(cache)
    fun paramIds(): List<Int> = params.ids(cache)
    fun varClientIds(): List<Int> = varClients.ids(cache)
    fun varBitIds(): List<Int> = varBits.ids(cache)
    fun dbRowIds(): List<Int> = dbRows.ids(cache)
    fun dbTableIds(): List<Int> = dbTables.ids(cache)
    fun floorOverlayIds(): List<Int> = floorOverlays.ids(cache)
    fun floorUnderlayIds(): List<Int> = floorUnderlays.ids(cache)
    fun headbarIds(): List<Int> = headbars.ids(cache)
    fun hitmarkIds(): List<Int> = hitmarks.ids(cache)
    fun lightIds(): List<Int> = lights.ids(cache)
    fun basIds(): List<Int> = bases.ids(cache)
    fun waterIds(): List<Int> = waters.ids(cache)
    fun worldAreaIds(): List<Int> = worldAreas.ids(cache)
    fun particleEmitterIds(): List<Int> = particleEmitters.ids(cache)
    fun particleEffectorIds(): List<Int> = particleEffectors.ids(cache)
    fun billboardIds(): List<Int> = billboards.ids(cache)

    fun <T : Definition> read(decoder: Rs2Decoder<T>, id: Int): T? =
        runCatching { decoder.read(cache, id) }.getOrNull()

    val locs = Rs2LocDecoder()
    val npcs = Rs2NpcDecoder()
    val objs = Rs2ObjDecoder()
    val seqs = Rs2SequenceDecoder()
    val spotAnims = Rs2SpotAnimDecoder()
    val identityKits = Rs2IdentityKitDecoder()
    val enums = Rs2EnumDecoder()
    val structs = Rs2StructDecoder()
    val params = Rs2ParamDecoder()
    val varClients = Rs2VarClientDecoder()
    val varBits = Rs2VarBitDecoder()
    val dbRows = Rs2DbRowDecoder()
    val dbTables = Rs2DbTableDecoder()
    val floorOverlays = Rs2FloorOverlayDecoder()
    val floorUnderlays = Rs2FloorUnderlayDecoder()
    val headbars = Rs2HeadbarDecoder()
    val hitmarks = Rs2HitmarkDecoder()
    val lights = Rs2LightDecoder()
    val bases = Rs2BasDecoder()
    val waters = Rs2WaterDecoder()
    val worldAreas = Rs2WorldAreaDecoder()
    val particleEmitters = Rs2ParticleEmitterDecoder()
    val particleEffectors = Rs2ParticleEffectorDecoder()
    val billboards = Rs2BillboardDecoder()
}
