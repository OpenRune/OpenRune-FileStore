package dev.openrune.seralizer

import dev.openrune.toml.TomlMapper
import dev.openrune.toml.model.TomlValue
import dev.openrune.toml.rsconfig.TypedRsTableRowPostDecode
import dev.openrune.definition.type.ItemSlotType
import dev.openrune.definition.type.ItemType
import dev.openrune.definition.type.MapElementType
import dev.openrune.definition.type.NpcType
import dev.openrune.definition.type.ObjectType

object ItemTypeOptionsTableHook : TypedRsTableRowPostDecode<ItemType>(ItemType::class) {

    @Suppress("UNUSED_PARAMETER")
    override fun applyTyped(mapper: TomlMapper, content: Map<String, TomlValue>, def: ItemType) {
        applyFromToml(content, def)
    }

    fun applyFromToml(content: Map<String, TomlValue>, def: ItemType) {
        content["equipmentType"]?.let { typeValue ->
            val type = (typeValue as? TomlValue.String)?.value ?: error("equipmentType must be a string")
            val slotType = ItemSlotType.fetchType(type) ?: error("Unable to find slot type for $type")
            def.apply {
                equipSlot = slotType.slot
                appearanceOverride1 = slotType.override1
                appearanceOverride2 = slotType.override2
            }
        }
        EntityOpsOptionsFromToml.apply(def.options, def.id, def.name, "option", content)
        StringListOptionsFromToml.apply(def.interfaceOptions, def.id, def.name, "ioption", content)
    }
}

object ObjectTypeOptionsTableHook : TypedRsTableRowPostDecode<ObjectType>(ObjectType::class) {

    @Suppress("UNUSED_PARAMETER")
    override fun applyTyped(mapper: TomlMapper, content: Map<String, TomlValue>, def: ObjectType) {
        applyFromToml(content, def)
    }

    fun applyFromToml(content: Map<String, TomlValue>, def: ObjectType) {
        EntityOpsOptionsFromToml.apply(def.actions, def.id, def.name, "option", content)
    }
}

object NpcTypeOptionsTableHook : TypedRsTableRowPostDecode<NpcType>(NpcType::class) {

    @Suppress("UNUSED_PARAMETER")
    override fun applyTyped(mapper: TomlMapper, content: Map<String, TomlValue>, def: NpcType) {
        applyFromToml(content, def)
    }

    fun applyFromToml(content: Map<String, TomlValue>, def: NpcType) {
        EntityOpsOptionsFromToml.apply(def.actions, def.id, def.name, "option", content)
    }
}

object MapElementTypeOptionsTableHook : TypedRsTableRowPostDecode<MapElementType>(MapElementType::class) {

    @Suppress("UNUSED_PARAMETER")
    override fun applyTyped(mapper: TomlMapper, content: Map<String, TomlValue>, def: MapElementType) {
        applyFromToml(content, def)
    }

    fun applyFromToml(content: Map<String, TomlValue>, def: MapElementType) {
        StringListOptionsFromToml.apply(def.options, def.id, def.name, "option", content)
    }
}
