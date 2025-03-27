package dev.openrune.definition.type

import dev.openrune.definition.util.Type
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
class DBColumnType(val types: Array<Type>, val values: Array<@Contextual Any>?)