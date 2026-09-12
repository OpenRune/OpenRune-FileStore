package dev.openrune.definition.type.builders

import dev.openrune.definition.type.*

import dev.openrune.definition.util.CacheVarLiteral

class VarClanSettingsTypeBuilder(var id: Int = -1) {

    var type: CacheVarLiteral? = null
    var lifetime: Int = 0
    var debugName: String = ""

    fun build(): VarClanSettingsType = VarClanSettingsType(
        id = id,
        type = type,
        lifetime = lifetime,
        debugName = debugName,
    )

    companion object {
        /** Copies every field of [type] into a fresh builder. */
        fun from(type: VarClanSettingsType): VarClanSettingsTypeBuilder {
            val builder = VarClanSettingsTypeBuilder(type.id)
            builder.type = type.type
            builder.lifetime = type.lifetime
            builder.debugName = type.debugName
            return builder
        }
    }
}
