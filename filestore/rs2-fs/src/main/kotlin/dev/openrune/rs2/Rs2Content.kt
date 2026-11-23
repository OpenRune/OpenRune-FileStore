package dev.openrune.rs2

enum class Rs2Content(
    val location: Rs2ConfigLocation,
    val index: Int,
    val bits: Int = -1,
    val group: Int = -1,
) {
    LOC(Rs2ConfigLocation.LOC, Rs2Index.CONFIG_LOC, bits = 8),
    ENUM(Rs2ConfigLocation.ENUM, Rs2Index.CONFIG_ENUM, bits = 8),
    NPC(Rs2ConfigLocation.NPC, Rs2Index.CONFIG_NPC, bits = 7),
    OBJ(Rs2ConfigLocation.OBJ, Rs2Index.CONFIG_OBJ, bits = 8),
    SEQ(Rs2ConfigLocation.SEQ, Rs2Index.CONFIG_SEQ, bits = 7),
    SPOT(Rs2ConfigLocation.SPOT, Rs2Index.CONFIG_SPOT, bits = 8),
    STRUCT(Rs2ConfigLocation.STRUCT, Rs2Index.CONFIG_STRUCT, bits = 5),

    FLOOR_UNDERLAY(Rs2ConfigLocation.FLOOR_UNDERLAY, Rs2Index.CONFIG, group = Rs2ConfigGroup.FLUTYPE),
    FLOOR_OVERLAY(Rs2ConfigLocation.FLOOR_OVERLAY, Rs2Index.CONFIG, group = Rs2ConfigGroup.FLOTYPE),
    IDK(Rs2ConfigLocation.IDK, Rs2Index.CONFIG, group = Rs2ConfigGroup.IDKTYPE),
    PARAM(Rs2ConfigLocation.PARAM, Rs2Index.CONFIG, group = Rs2ConfigGroup.PARAMTYPE),
    VAR_CLIENT(Rs2ConfigLocation.VAR_CLIENT, Rs2Index.CONFIG, group = Rs2ConfigGroup.VARCLIENTTYPE),
    HITMARK(Rs2ConfigLocation.HITMARK, Rs2Index.CONFIG, group = Rs2ConfigGroup.HITMARKTYPE),
    DBROW(Rs2ConfigLocation.DBROW, Rs2Index.CONFIG, group = Rs2ConfigGroup.DBROWTYPE),
    DBTABLE(Rs2ConfigLocation.DBTABLE, Rs2Index.CONFIG, group = Rs2ConfigGroup.DBTABLETYPE),
    VAR_BIT(Rs2ConfigLocation.VAR_BIT, Rs2Index.CONFIG, group = Rs2ConfigGroup.VAR_BIT),
    HEADBAR(Rs2ConfigLocation.HEADBAR, Rs2Index.CONFIG, group = Rs2ConfigGroup.HEADBARTYPE),
    LIGHT(Rs2ConfigLocation.LIGHT, Rs2Index.CONFIG, group = Rs2ConfigGroup.LIGHTTYPE),
    BAS(Rs2ConfigLocation.BAS, Rs2Index.CONFIG, group = Rs2ConfigGroup.BASTYPE),
    WATER(Rs2ConfigLocation.WATER, Rs2Index.CONFIG, group = Rs2ConfigGroup.WATERTYPE),
    WORLD_AREA(Rs2ConfigLocation.WORLD_AREA, Rs2Index.CONFIG, group = Rs2ConfigGroup.WORLDAREATYPE),
    ;

    val promoted: Boolean get() = bits >= 0

    fun archiveOf(id: Int): Int = if (promoted) id shr bits else group

    fun fileOf(id: Int): Int = if (promoted) id and ((1 shl bits) - 1) else id
}
