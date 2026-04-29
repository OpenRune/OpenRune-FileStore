package dev.openrune.seralizer

import dev.openrune.toml.TomlMapper
import dev.openrune.toml.model.TomlValue
import dev.openrune.toml.rsconfig.TypedRsTableRowPostDecode
import dev.openrune.definition.type.ParamType
import dev.openrune.definition.util.BaseVarType

object ParamTypeTableHook : TypedRsTableRowPostDecode<ParamType>(ParamType::class) {

    @Suppress("UNUSED_PARAMETER")
    override fun applyTyped(mapper: TomlMapper, content: Map<String, TomlValue>, def: ParamType) {
        val intSet = def.defaultInt != 0
        val strSet = !def.defaultString.isNullOrBlank()
        val longSet = def.defaultLong != 0L
        val count = (if (intSet) 1 else 0) + (if (strSet) 1 else 0) + (if (longSet) 1 else 0)

        require(count <= 1) {
            "ParamType id=${def.id}: only one of defaultInt, defaultString, defaultLong may be set"
        }

        val t = def.type ?: return
        if (count == 0) return

        val expectedDefaultField = when (t.baseType) {
            BaseVarType.INTEGER -> "defaultInt"
            BaseVarType.LONG -> "defaultLong"
            BaseVarType.STRING -> "defaultString"
            BaseVarType.ARRAY -> "none (array params cannot use scalar defaults)"
        }
        val providedDefaults = buildList {
            if (intSet) add("defaultInt")
            if (longSet) add("defaultLong")
            if (strSet) add("defaultString")
        }

        val matches = when (t.baseType) {
            BaseVarType.INTEGER -> intSet
            BaseVarType.LONG -> longSet
            BaseVarType.STRING -> strSet
            BaseVarType.ARRAY -> false
        }
        require(matches) {
            "ParamType id=${def.id}: type '${t.name}' is ${t.baseType}; expected $expectedDefaultField, got ${providedDefaults.joinToString(", ")}"
        }
    }
}