package dev.openrune.definition.constants

import java.io.File

interface MutableMappingProvider : MappingProvider {

    fun unassignedGameVals(): List<UnassignedGameVal>

    fun maxBaseId(table: String): Int = -1

    fun sourceOf(table: String, key: String): File?

    fun writeGameVals(entries: List<GameValWrite>)
}

data class UnassignedGameVal(
    val table: String,
    val key: String,
    val source: File,
)

data class GameValWrite(
    val table: String,
    val key: String,
    val id: Int,
    val source: File? = null,
    val after: String? = null,
    val generated: Boolean = false,
)
