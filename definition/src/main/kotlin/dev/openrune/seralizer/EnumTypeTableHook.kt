package dev.openrune.seralizer

import dev.openrune.toml.TomlMapper
import dev.openrune.toml.model.TomlValue
import dev.openrune.toml.rsconfig.TypedRsTableRowPostDecode
import dev.openrune.definition.type.EnumType
import dev.openrune.definition.util.BaseVarType

object EnumTypeTableHook : TypedRsTableRowPostDecode<EnumType>(EnumType::class) {

    @Suppress("UNUSED_PARAMETER")
    override fun applyTyped(mapper: TomlMapper, content: Map<String, TomlValue>, def: EnumType) {
        val intSet = def.defaultInt != 0
        val strSet = def.defaultString.isNotEmpty()
        val count = (if (intSet) 1 else 0) + (if (strSet) 1 else 0)

        val valueBase = def.valueType.baseType
        val expectedDefaultField = when (valueBase) {
            BaseVarType.STRING -> "defaultString"
            BaseVarType.INTEGER, BaseVarType.LONG -> "defaultInt"
            BaseVarType.ARRAY -> "none (enum defaults cannot be array)"
        }
        val removeField = when (valueBase) {
            BaseVarType.STRING -> "defaultInt"
            BaseVarType.INTEGER, BaseVarType.LONG -> "defaultString"
            BaseVarType.ARRAY -> "defaultInt and defaultString"
        }
        val providedDefaults = buildList {
            if (intSet) add("defaultInt")
            if (strSet) add("defaultString")
        }
        require(count <= 1) {
            "EnumType id=${def.id}: both defaultInt and defaultString are set for valueType '${def.valueType.name}' ($valueBase). Remove $removeField."
        }
        val defaultMatches = when (valueBase) {
            BaseVarType.STRING -> strSet
            BaseVarType.INTEGER, BaseVarType.LONG -> intSet
            BaseVarType.ARRAY -> false
        }
        require(defaultMatches || providedDefaults.isEmpty()) {
            "EnumType id=${def.id}: valueType '${def.valueType.name}' is $valueBase; expected $expectedDefaultField, got ${providedDefaults.joinToString(", ")}"
        }

        def.values.forEach { (key, value) ->
            require(matchesBaseType(key, def.keyType.baseType)) {
                "EnumType id=${def.id}: key '$key' does not match keyType '${def.keyType.name}' (${def.keyType.baseType})"
            }
            require(matchesBaseType(value, def.valueType.baseType)) {
                "EnumType id=${def.id}: value '$value' for key '$key' does not match valueType '${def.valueType.name}' (${def.valueType.baseType})"
            }
        }
    }

    private fun matchesBaseType(value: Any, baseType: BaseVarType): Boolean =
        when (baseType) {
            BaseVarType.INTEGER -> value.toString().toIntOrNull() != null
            BaseVarType.LONG -> value.toString().toLongOrNull() != null
            BaseVarType.STRING -> value is String
            BaseVarType.ARRAY -> false
        }
}
