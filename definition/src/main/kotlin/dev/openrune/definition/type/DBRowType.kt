package dev.openrune.definition.type

import dev.openrune.definition.Definition
import dev.openrune.definition.util.CacheVarLiteral

class DBRowType(
    override var id: Int = -1,
    var rscmName: String? = null,
) : Definition {

    var columnTypes: Array<Array<Any?>?>? = null
    var field5306: Array<IntArray?>? = null

    var tableId: Int = -1

    fun ensureColumnStorage(length: Int) {
        if (length <= 0) return
        if (columnTypes == null || columnTypes!!.size < length) {
            columnTypes = arrayOfNulls(length)
            field5306 = arrayOfNulls(length)
        }
    }

    fun definedColumns(): Map<Int, DBColumnType> {
        val ct = columnTypes ?: return emptyMap()
        val fs = field5306 ?: return emptyMap()
        val out = mutableMapOf<Int, DBColumnType>()
        for (i in ct.indices) {
            val raw = ct[i] ?: continue
            val ids = fs[i] ?: continue
            val types = Array(ids.size) { idx -> CacheVarLiteral.byID(ids[idx]) }
            out[i] = DBColumnType(types, raw.requireNoNulls())
        }
        return out
    }

    override fun toString(): String =
        "DBRowType(id=$id, tableId=$tableId, rscmName=$rscmName, definedColumns=${definedColumns()})"
}
