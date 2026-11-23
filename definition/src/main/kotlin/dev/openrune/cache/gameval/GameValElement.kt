package dev.openrune.cache.gameval

open class GameValElement(
    open val name: String,
    open val id: Int
) {
    open fun toFullString(): String = "$name:$id"

}