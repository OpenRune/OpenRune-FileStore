package dev.openrune.definition

object Js5GameValGroup {
    const val OBJTYPES = 0
    const val NPCTYPES = 1
    const val INVTYPES = 2
    const val VARPTYPES = 3
    const val VARBITTYPES = 4
    const val LOCTYPES = 6
    const val SEQTYPES = 7
    const val SPOTTYPES = 8
    const val ROWTYPES = 9
    const val TABLETYPES = 10
    const val SOUNDTYPES = 11
    const val SPRITETYPES = 12
    const val IFTYPES = 13

    fun toString(type: Int): String = when (type) {
        OBJTYPES -> "objtypes"
        NPCTYPES -> "npctypes"
        INVTYPES -> "invtypes"
        VARPTYPES -> "varptypes"
        VARBITTYPES -> "varbittypes"
        LOCTYPES -> "loctypes"
        SEQTYPES -> "seqtypes"
        SPOTTYPES -> "spottypes"
        ROWTYPES -> "rowtypes"
        TABLETYPES -> "tabletypes"
        SOUNDTYPES -> "soundtypes"
        SPRITETYPES -> "spritetypes"
        IFTYPES -> "iftypes"
        else -> error("Unknown group type: $type")
    }
}