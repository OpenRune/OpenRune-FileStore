package dev.openrune.definition.type

import dev.openrune.definition.util.CacheVarLiteral

class DBColumnType(val types: Array<CacheVarLiteral>, val values: Array<Any>?, val rscmName: String? = null) {
    override fun toString(): String {
        return "DBColumnType(types=${types.contentToString()}, values=${values?.contentToString()}, rscmName=$rscmName)"
    }
}