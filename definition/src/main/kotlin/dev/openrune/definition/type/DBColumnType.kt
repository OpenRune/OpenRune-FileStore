package dev.openrune.definition.type

import dev.openrune.definition.util.VarType

class DBColumnType(val types: Array<VarType>, val values: Array<Any>?) {
    override fun toString(): String {
        return "DBColumnType(types=${types.contentToString()}, values=${values?.contentToString()})"
    }
}