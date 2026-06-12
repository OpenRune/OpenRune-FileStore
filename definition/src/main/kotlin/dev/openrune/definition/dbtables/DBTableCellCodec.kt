package dev.openrune.definition.dbtables

import dev.openrune.definition.constants.ConstantProvider
import dev.openrune.definition.util.BaseVarType
import dev.openrune.definition.util.CacheVarLiteral

/**
 * Maps column [CacheVarLiteral] types to DSL semantics: [DBRowBuilder.columnRSCM] vs [DBRowBuilder.column].
 *
 * String cell values resolve through [ConstantProvider] only when the column type is a cache reference
 * (OBJ, STAT, ENUM, …). [CacheVarLiteral.STRING], [CacheVarLiteral.INT], and [CacheVarLiteral.BOOLEAN]
 * keep literals / raw numbers.
 */
internal object DBTableCellCodec {

    fun usesRscmMapping(type: CacheVarLiteral): Boolean = when (type.name) {
        "STRING", "INT", "BOOLEAN" -> false
        else -> type.baseType != BaseVarType.STRING
    }

    fun decodeString(raw: String, type: CacheVarLiteral): Any {
        if (!usesRscmMapping(type)) return raw
        return ConstantProvider.getMapping(raw)
    }

    fun encodeInt(value: Int, type: CacheVarLiteral): String {
        if (type.name == "BOOLEAN") return (value != 0).toString()
        if (!usesRscmMapping(type)) return value.toString()
        return reverseLookup(value, preferredTables = preferredTablesFor(type))?.let { "\"$it\"" }
            ?: value.toString()
    }

    fun encodeTableId(tableId: Int, rscmName: String?): String {
        if (rscmName != null) {
            for (prefix in tableIdPrefixes) {
                val key = "$prefix.$rscmName"
                if (ConstantProvider.getMappingOrNull(key) == tableId) {
                    return "\"$key\""
                }
            }
        }
        reverseLookup(tableId, tableIdPrefixes)?.let { return "\"$it\"" }
        return tableId.toString()
    }

    fun encodeRowId(rowId: Int, rscmName: String?): String {
        if (rscmName != null) {
            for (prefix in rowIdPrefixes) {
                val key = "$prefix.$rscmName"
                if (ConstantProvider.getMappingOrNull(key) == rowId) {
                    return "\"$key\""
                }
            }
        }
        reverseLookup(rowId, rowIdPrefixes)?.let { return "\"$it\"" }
        return rowId.toString()
    }

    private val tableIdPrefixes = listOf("dbtable", "dbtables", "tables", "table")
    private val rowIdPrefixes = listOf("dbrow", "dbrows", "row", "rows")

    private fun preferredTablesFor(type: CacheVarLiteral): List<String> = when (type.name) {
        "OBJ", "NAMEDOBJ" -> listOf("objects", "object", "objs", "obj")
        "NPC" -> listOf("npcs", "npc")
        "LOC" -> listOf("locs", "loc", "objects", "object")
        "STAT" -> listOf("stats", "stat")
        "ENUM" -> listOf("enums", "enum")
        "SEQ" -> listOf("seqs", "seq")
        "CATEGORY", "CATEGORYTYPE" -> listOf("categories", "category")
        "DBROW" -> rowIdPrefixes
        "VARBIT" -> listOf("varbits", "varbit")
        else -> emptyList()
    }

    fun reverseLookup(id: Int, preferredTables: List<String> = emptyList()): String? {
        if (!isConstantProviderLoaded()) return null
        for (table in preferredTables) {
            ConstantProvider.mappings[table]?.entries?.find { it.value == id }?.let { return it.key }
        }
        for ((_, entries) in ConstantProvider.mappings) {
            entries.entries.find { it.value == id }?.let { return it.key }
        }
        return null
    }

    fun isConstantProviderLoaded(): Boolean =
        runCatching { ConstantProvider.getMappingOrNull("__probe__.missing") }.isSuccess
}
