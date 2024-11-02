package dev.openrune.cache.filestore.definition.data

import dev.openrune.cache.filestore.definition.Definition
import dev.openrune.cache.util.ScriptVarType
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap

class ParamType(
    override var id: Int = -1,
    override val values: MutableMap<String, Any> = Object2ObjectOpenHashMap(),
    //Custom
    override var inherit: Int = -1
) : Definition {

    fun getType(): ScriptVarType? {
        return values["type"] as? ScriptVarType
    }

    fun isMembers(): Boolean {
        return values.getOrDefault("isMembers", true) as Boolean
    }

    fun getDefaultInt(): Int {
        return values.getOrDefault("defaultInt", 0) as Int
    }

    fun getDefaultString(): String? {
        return values["defaultString"]?.toString()
    }
}

