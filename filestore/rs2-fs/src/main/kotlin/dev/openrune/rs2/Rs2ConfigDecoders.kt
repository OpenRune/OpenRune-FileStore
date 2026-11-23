package dev.openrune.rs2

import dev.openrune.definition.codec.BasCodec
import dev.openrune.definition.codec.BillboardCodec
import dev.openrune.definition.codec.FloorOverlayCodec
import dev.openrune.definition.codec.FloorUnderlayCodec
import dev.openrune.definition.codec.HeadbarCodec
import dev.openrune.definition.codec.HitmarkCodec
import dev.openrune.definition.codec.LightCodec
import dev.openrune.definition.codec.ParticleEffectorCodec
import dev.openrune.definition.codec.ParticleEmitterCodec
import dev.openrune.definition.codec.Rs2DbRowCodec
import dev.openrune.definition.codec.Rs2DbTableCodec
import dev.openrune.definition.codec.Rs2EnumCodec
import dev.openrune.definition.codec.Rs2IdentityKitCodec
import dev.openrune.definition.codec.Rs2LocCodec
import dev.openrune.definition.codec.Rs2NpcCodec
import dev.openrune.definition.codec.Rs2ObjCodec
import dev.openrune.definition.codec.Rs2ParamCodec
import dev.openrune.definition.codec.Rs2SequenceCodec
import dev.openrune.definition.codec.Rs2SpotAnimCodec
import dev.openrune.definition.codec.Rs2StructCodec
import dev.openrune.definition.codec.Rs2VarBitCodec
import dev.openrune.definition.codec.Rs2VarClientCodec
import dev.openrune.definition.codec.WaterCodec
import dev.openrune.definition.codec.WorldAreaCodec
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

class Rs2LocDecoder : Rs2ConfigDecoder<Rs2LocType>(Rs2ConfigLocation.LOC) {
    override fun codecFor(build: Int) = Rs2LocCodec(build)
}

class Rs2NpcDecoder : Rs2ConfigDecoder<Rs2NpcType>(Rs2ConfigLocation.NPC) {
    override fun codecFor(build: Int) = Rs2NpcCodec(build)
}

class Rs2ObjDecoder : Rs2ConfigDecoder<Rs2ObjType>(Rs2ConfigLocation.OBJ) {
    override fun codecFor(build: Int) = Rs2ObjCodec(build)
}

class Rs2SequenceDecoder : Rs2ConfigDecoder<Rs2SequenceType>(Rs2ConfigLocation.SEQ) {
    override fun codecFor(build: Int) = Rs2SequenceCodec(build)
}

class Rs2SpotAnimDecoder : Rs2ConfigDecoder<Rs2SpotAnimType>(Rs2ConfigLocation.SPOT) {
    override fun codecFor(build: Int) = Rs2SpotAnimCodec(build)
}

class Rs2IdentityKitDecoder : Rs2ConfigDecoder<Rs2IdentityKitType>(Rs2ConfigLocation.IDK) {
    override fun codecFor(build: Int) = Rs2IdentityKitCodec(build)
}

class Rs2EnumDecoder : Rs2ConfigDecoder<EnumType>(Rs2ConfigLocation.ENUM) {
    override fun codecFor(build: Int) = Rs2EnumCodec()
}

class Rs2StructDecoder : Rs2ConfigDecoder<StructType>(Rs2ConfigLocation.STRUCT) {
    override fun codecFor(build: Int) = Rs2StructCodec()
}

class Rs2ParamDecoder : Rs2ConfigDecoder<ParamType>(Rs2ConfigLocation.PARAM) {
    override fun codecFor(build: Int) = Rs2ParamCodec()
}

class Rs2VarClientDecoder : Rs2ConfigDecoder<VarClientType>(Rs2ConfigLocation.VAR_CLIENT) {
    override fun codecFor(build: Int) = Rs2VarClientCodec()
}

class Rs2VarBitDecoder : Rs2ConfigDecoder<Rs2VarBitType>(Rs2ConfigLocation.VAR_BIT) {
    override fun codecFor(build: Int) = Rs2VarBitCodec()
}

class Rs2DbRowDecoder : Rs2ConfigDecoder<Rs2DbRowType>(Rs2ConfigLocation.DBROW) {
    override fun codecFor(build: Int) = Rs2DbRowCodec()
}

class Rs2DbTableDecoder : Rs2ConfigDecoder<Rs2DbTableType>(Rs2ConfigLocation.DBTABLE) {
    override fun codecFor(build: Int) = Rs2DbTableCodec()
}

class Rs2FloorOverlayDecoder : Rs2ConfigDecoder<FloorOverlayType>(Rs2ConfigLocation.FLOOR_OVERLAY) {
    override fun codecFor(build: Int) = FloorOverlayCodec(build)
}

class Rs2FloorUnderlayDecoder : Rs2ConfigDecoder<FloorUnderlayType>(Rs2ConfigLocation.FLOOR_UNDERLAY) {
    override fun codecFor(build: Int) = FloorUnderlayCodec()
}

class Rs2HeadbarDecoder : Rs2ConfigDecoder<HeadbarType>(Rs2ConfigLocation.HEADBAR) {
    override fun codecFor(build: Int) = HeadbarCodec()
}

class Rs2HitmarkDecoder : Rs2ConfigDecoder<HitmarkType>(Rs2ConfigLocation.HITMARK) {
    override fun codecFor(build: Int) = HitmarkCodec()
}

class Rs2LightDecoder : Rs2ConfigDecoder<LightType>(Rs2ConfigLocation.LIGHT) {
    override fun codecFor(build: Int) = LightCodec()
}

class Rs2BasDecoder : Rs2ConfigDecoder<BasType>(Rs2ConfigLocation.BAS) {
    override fun codecFor(build: Int) = BasCodec(build)
}

class Rs2WaterDecoder : Rs2ConfigDecoder<WaterType>(Rs2ConfigLocation.WATER) {
    override fun codecFor(build: Int) = WaterCodec()
}

class Rs2WorldAreaDecoder : Rs2ConfigDecoder<WorldAreaType>(Rs2ConfigLocation.WORLD_AREA) {
    override fun codecFor(build: Int) = WorldAreaCodec()
}

class Rs2ParticleEmitterDecoder :
    Rs2ConfigDecoder<ParticleEmitterType>(Rs2ConfigLocation.PARTICLE_EMITTER) {
    override fun codecFor(build: Int) = ParticleEmitterCodec()
}

class Rs2ParticleEffectorDecoder :
    Rs2ConfigDecoder<ParticleEffectorType>(Rs2ConfigLocation.PARTICLE_EFFECTOR) {
    override fun codecFor(build: Int) = ParticleEffectorCodec()
}

class Rs2BillboardDecoder : Rs2ConfigDecoder<BillboardType>(Rs2ConfigLocation.BILLBOARD) {
    override fun codecFor(build: Int) = BillboardCodec()
}
