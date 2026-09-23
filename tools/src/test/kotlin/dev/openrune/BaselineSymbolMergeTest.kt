package dev.openrune

import dev.openrune.cache.tools.cs2.PackCs2
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** Locks how the pre-build symbol snapshot is merged with this build's tables for Neptune's library baseline. */
class BaselineSymbolMergeTest {

    @Test
    fun `snapshot ids win and typeless snapshot lines borrow the current type column`() {
        val current = listOf(
            "0\tmcannon\tint",
            "10\tpagepos\tint",
            "11\tsailing_bt_trial_data_if\tdbrow",
            "12\tnew_this_build\tint",
        )
        val snapshot = listOf(
            "0\tmcannon",
            "9\tpagepos",
            "11\tsailing_bt_trial_data_if",
        )

        val merged = PackCs2.mergeBaselineSymbols(current, snapshot)

        assertEquals(
            listOf(
                "0\tmcannon\tint",
                "9\tpagepos\tint",
                "11\tsailing_bt_trial_data_if\tdbrow",
                "12\tnew_this_build\tint",
            ),
            merged,
        )
    }

    @Test
    fun `names containing spaces are not split into a type column`() {
        val current = listOf("53:3\tburgh_map:temple hi model 1", "53:4\tburgh_map:other name")
        val snapshot = listOf("53:2\tburgh_map:temple hi model 1")

        assertEquals(
            listOf("53:2\tburgh_map:temple hi model 1", "53:4\tburgh_map:other name"),
            PackCs2.mergeBaselineSymbols(current, snapshot),
        )
    }

    @Test
    fun `snapshot line keeps its own columns and current lines colliding with snapshot ids are dropped`() {
        val current = listOf("5\trenamed\tint", "7\tunrelated")
        val snapshot = listOf("5\toriginal\tcoord")

        assertEquals(listOf("5\toriginal\tcoord", "7\tunrelated"), PackCs2.mergeBaselineSymbols(current, snapshot))
    }
}
