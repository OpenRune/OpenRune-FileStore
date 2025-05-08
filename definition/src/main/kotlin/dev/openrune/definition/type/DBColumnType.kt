package dev.openrune.definition.type

import dev.openrune.definition.util.Type

class DBColumnType(val types: Array<Type>, val values: Array<Any>?) {
    override fun toString(): String {
        return "DBColumnType(types=${types.contentToString()}, values=${values?.contentToString()})"
    }
}