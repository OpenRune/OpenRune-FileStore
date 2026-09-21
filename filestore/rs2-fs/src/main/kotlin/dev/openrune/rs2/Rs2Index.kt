package dev.openrune.rs2

/**
 * Top-level JS5 archive (index) ids for the modern RS2/RS3 cache layout.
 *
 * Sourced from [zwyz/rs3-cache](https://github.com/zwyz/rs3-cache)'s
 * `Js5Archive` enum. Early ids (0-15) match the legacy/OSRS layout; ids from
 * 16 onwards are where RS3 split large config groups (locs, npcs, objs, seqs,
 * spotanims, structs, enums, achievements) out into their own top-level index.
 */
object Rs2Index {
    const val ANIMS = 0
    const val BASES = 1
    const val CONFIG = 2
    const val INTERFACES = 3
    const val SYNTH = 4
    const val MAPS = 5
    const val SONGS = 6
    const val MODELS = 7
    const val SPRITES = 8
    const val TEXTURES = 9
    const val BINARY = 10
    const val JINGLES = 11
    const val CLIENTSCRIPTS = 12
    const val FONTMETRICS = 13
    const val VORBIS = 14
    const val MIDIPATCHES = 15

    // RS3-only: config groups promoted to their own top-level index.
    const val CONFIG_LOC = 16
    const val CONFIG_ENUM = 17
    const val CONFIG_NPC = 18
    const val CONFIG_OBJ = 19
    const val CONFIG_SEQ = 20
    const val CONFIG_SPOT = 21
    const val CONFIG_STRUCT = 22

    const val WORLDMAPDATA = 23
    const val QUICKCHAT = 24
    const val MATERIALS = 26
    const val CONFIG_PARTICLE = 27
    const val DEFAULTS = 28
    const val CONFIG_BILLBOARD = 29

    const val AUDIOSTREAMS = 40
    const val WORLDMAPAREADATA = 41
    const val WORLDMAPLABELS = 42
    const val MODELS_RT7 = 47
    const val ANIMS_RT7 = 48
    const val DBTABLEINDEX = 49
    const val TEXTURES_DXT = 52
    const val TEXTURES_PNG_MIPPED = 54
    const val TEXTURES_ETC = 55
    const val KEYFRAMES = 56
    const val CONFIG_ACHIEVEMENT = 57
    const val FONTMETRICS_RT7 = 58
    const val TRUETYPEFONTS = 59
    const val STYLESHEETS = 60
    const val VFX = 61
    const val ANIMSTATEMACHINES = 62
    const val UI_ANIMATIONS = 65
    const val CUTSCENES2D = 66
    const val GAMEVALS = 67
}
