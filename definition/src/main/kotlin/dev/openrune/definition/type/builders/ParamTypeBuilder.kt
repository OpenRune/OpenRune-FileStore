package dev.openrune.definition.type.builders

import dev.openrune.definition.type.*

import dev.openrune.definition.util.CacheVarLiteral

/**
 * The mutable side of [ParamType]. Codecs decode into one of these and the packing tools edit
 * one — either fresh or via [ParamType.toBuilder] — then [build] produces the immutable
 * definition everything else reads.
 */
class ParamTypeBuilder(var id: Int = -1) {

    var type: CacheVarLiteral? = null
    var isMembers: Boolean = true
    var defaultInt: Int = 0
    var defaultString: String? = null
    var defaultLong: Long = 0L

    fun build(): ParamType = ParamType(
        id = id,
        type = type,
        isMembers = isMembers,
        defaultInt = defaultInt,
        defaultString = defaultString,
        defaultLong = defaultLong,
    )

    companion object {
        /** Copies every field of [type] into a fresh builder. */
        fun from(type: ParamType): ParamTypeBuilder {
            val builder = ParamTypeBuilder(type.id)
            builder.type = type.type
            builder.isMembers = type.isMembers
            builder.defaultInt = type.defaultInt
            builder.defaultString = type.defaultString
            builder.defaultLong = type.defaultLong
            return builder
        }
    }
}
