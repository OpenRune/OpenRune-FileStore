package dev.openrune.definition.type.builders

import dev.openrune.definition.type.*
import dev.openrune.definition.util.CacheVarLiteral

class VarClientTypeBuilder(var id: Int = -1) {

    var persist: Boolean = false
    var type: CacheVarLiteral? = null

    fun build(): VarClientType = VarClientType(
        id = id,
        persist = persist,
    ).also { built -> type?.let { built.rs2 = VarClientType.Rs2(it) } }

    companion object {
        /** Copies every field of [type] into a fresh builder. */
        fun from(type: VarClientType): VarClientTypeBuilder {
            val builder = VarClientTypeBuilder(type.id)
            builder.persist = type.persist
            builder.type = type.rs2?.type
            return builder
        }
    }
}
