package dev.openrune.cache.tools.worldmap.utils

data class Tuple5<T1, T2, T3, T4, T5>(val t1: T1, val t2: T2, val t3: T3, val t4: T4, val t5: T5) {
    override fun toString(): String = "($t1, $t2, $t3, $t4, $t5)"
}