package dev.openrune.cache.filestore.definition.data

import dev.openrune.cache.filestore.definition.Definition
import dev.openrune.cache.filestore.definition.Extra

data class StructType(
    override var id: Int = -1,
    override var stringId: String = "",
    override var extras: Map<String, Any>? = null,

    override var inherit: Int = -1
) : Definition, Extra {
    companion object {
        val EMPTY = StructType()
    }
}