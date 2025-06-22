package dev.openrune.cache.gameval.impl

import dev.openrune.cache.gameval.GameValElement

data class Sprite(
    override val name: String,
    val index: Int,
    override val id: Int
) : GameValElement(name, id) {
    override fun toFullString(): String = if (index == -1) "$name:$id" else "$name,$index:$id"
}