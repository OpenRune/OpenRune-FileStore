package dev.openrune.cache.tools

enum class SpriteNaming {
    ID,

    GAMEVAL,

    NAME,

    ID_AND_GAMEVAL,

    ID_AND_NAME;

    val usesGameVal: Boolean get() = this == GAMEVAL || this == ID_AND_GAMEVAL

    val prefixesId: Boolean get() = this == ID_AND_GAMEVAL || this == ID_AND_NAME
}
