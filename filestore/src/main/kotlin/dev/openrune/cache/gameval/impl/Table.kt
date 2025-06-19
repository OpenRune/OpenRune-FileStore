package dev.openrune.cache.gameval.impl

import dev.openrune.cache.gameval.GameValElement

data class Table(
    override val name: String,
    override val id: Int,
    val columns: List<Column> = emptyList()
) : GameValElement(name, id) {

    data class Column(override val name: String, override val id: Int) : GameValElement(name, id)

    override fun toFullString() = "$name:$id"
}