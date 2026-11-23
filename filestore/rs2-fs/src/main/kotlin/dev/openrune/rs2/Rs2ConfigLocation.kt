package dev.openrune.rs2

data class Rs2ConfigLocation(
    val group: Group? = null,
    val archive: Archive? = null,
) {
    data class Group(val index: Int, val group: Int)

    data class Archive(val index: Int, val bits: Int)

    companion object {
        private fun both(group: Int, archiveIndex: Int, bits: Int) = Rs2ConfigLocation(
            group = Group(Rs2Index.CONFIG, group),
            archive = Archive(archiveIndex, bits),
        )

        private fun config(group: Int) = Rs2ConfigLocation(group = Group(Rs2Index.CONFIG, group))

        private fun group(index: Int, group: Int) = Rs2ConfigLocation(group = Group(index, group))

        val LOC = both(Rs2ConfigGroup.LOCTYPE, Rs2Index.CONFIG_LOC, 8)
        val ENUM = both(Rs2ConfigGroup.ENUMTYPE, Rs2Index.CONFIG_ENUM, 8)
        val NPC = both(Rs2ConfigGroup.NPCTYPE, Rs2Index.CONFIG_NPC, 7)
        val OBJ = both(Rs2ConfigGroup.OBJTYPE, Rs2Index.CONFIG_OBJ, 8)
        val SEQ = both(Rs2ConfigGroup.SEQTYPE, Rs2Index.CONFIG_SEQ, 7)
        val SPOT = both(Rs2ConfigGroup.SPOTTYPE, Rs2Index.CONFIG_SPOT, 8)
        val STRUCT = both(Rs2ConfigGroup.STRUCTTYPE, Rs2Index.CONFIG_STRUCT, 5)

        val FLOOR_UNDERLAY = config(Rs2ConfigGroup.FLUTYPE)
        val IDK = config(Rs2ConfigGroup.IDKTYPE)
        val FLOOR_OVERLAY = config(Rs2ConfigGroup.FLOTYPE)
        val PARAM = config(Rs2ConfigGroup.PARAMTYPE)
        val VAR_CLIENT = config(Rs2ConfigGroup.VARCLIENTTYPE)
        val VAR_BIT = config(Rs2ConfigGroup.VAR_BIT)
        val LIGHT = config(Rs2ConfigGroup.LIGHTTYPE)
        val BAS = config(Rs2ConfigGroup.BASTYPE)
        val DBTABLE = config(Rs2ConfigGroup.DBTABLETYPE)
        val DBROW = config(Rs2ConfigGroup.DBROWTYPE)
        val HITMARK = config(Rs2ConfigGroup.HITMARKTYPE)
        val HEADBAR = config(Rs2ConfigGroup.HEADBARTYPE)
        val WATER = config(Rs2ConfigGroup.WATERTYPE)
        val WORLD_AREA = config(Rs2ConfigGroup.WORLDAREATYPE)

        val PARTICLE_EMITTER = group(Rs2Index.CONFIG_PARTICLE, 0)
        val PARTICLE_EFFECTOR = group(Rs2Index.CONFIG_PARTICLE, 1)
        val BILLBOARD = group(Rs2Index.CONFIG_BILLBOARD, 0)
    }
}
