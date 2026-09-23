package dev.openrune.cache.tools.gameval

import dev.openrune.cache.gameval.GameValElement
import dev.openrune.cache.gameval.impl.Interface
import dev.openrune.cache.gameval.impl.Sprite
import dev.openrune.cache.gameval.impl.Table
import dev.openrune.definition.GameValGroupTypes
import dev.openrune.definition.constants.ConstantProvider
import dev.openrune.definition.constants.MappingProvider
import java.io.File

/**
 * Feeds gamevals into [ConstantProvider] so `table.key` references resolve during the build.
 *
 * Every gameval a task registers is published the moment it is registered, so a task running later in
 * the build (configs referencing a freshly packed component, an enum pointing at a new dbrow) resolves
 * the id that is actually being written, not whatever a previous build left in the gameval files.
 *
 * Table names follow the gameval / RSCM convention (`obj`, `npc`, `loc`, `component`, `dbrow`, ...),
 * matching what the server-side gameval provider loads, so the two never disagree on a key.
 */
object GameValPublisher {

    /** Key of a nested element: `interface:component` or `table:column`. */
    private const val NESTED_SEPARATOR = ':'

    fun tableOf(group: GameValGroupTypes): String = when (group) {
        GameValGroupTypes.OBJTYPES -> "obj"
        GameValGroupTypes.NPCTYPES -> "npc"
        GameValGroupTypes.INVTYPES -> "inv"
        GameValGroupTypes.VARPTYPES -> "varp"
        GameValGroupTypes.VARBITTYPES -> "varbit"
        GameValGroupTypes.LOCTYPES -> "loc"
        GameValGroupTypes.SEQTYPES -> "seq"
        GameValGroupTypes.SPOTTYPES -> "spotanim"
        GameValGroupTypes.ROWTYPES -> "dbrow"
        GameValGroupTypes.TABLETYPES -> "dbtable"
        GameValGroupTypes.SOUNDTYPES -> "jingle"
        GameValGroupTypes.SPRITETYPES -> "sprites"
        GameValGroupTypes.IFTYPES, GameValGroupTypes.IFTYPES_V2 -> "interface"
        GameValGroupTypes.VARCS -> "varcs"
    }

    const val COMPONENT_TABLE = "component"
    const val DBCOL_TABLE = "dbcol"

    /**
     * Publishes [element] (and its components or columns) under the table for [group].
     * With [overwrite] the packed id replaces whatever the provider already held for the key;
     * without it the provider's value wins, which is what seeding from the cache wants.
     */
    fun publish(group: GameValGroupTypes, element: GameValElement, overwrite: Boolean = true): Int {
        ensureProvider()
        var count = 0
        fun put(table: String, key: String, id: Int) {
            if (key.isBlank()) return
            if (!overwrite && ConstantProvider.peekMapping("$table.$key") != null) return
            ConstantProvider.putMapping(table, key, id)
            count++
        }

        val table = tableOf(group)
        when (element) {
            is Sprite -> put(table, if (element.index == -1) element.name else "${element.name}$NESTED_SEPARATOR${element.index}", element.id)
            is Interface -> {
                put(table, element.name, element.id)
                element.components.forEach { component ->
                    put(COMPONENT_TABLE, "${element.name}$NESTED_SEPARATOR${component.name}", component.packed)
                }
            }
            is Table -> {
                put(table, element.name, element.id)
                element.columns.forEach { column ->
                    put(DBCOL_TABLE, "${element.name}$NESTED_SEPARATOR${column.name}", (element.id shl 16) or column.id)
                }
            }
            else -> put(table, element.name, element.id)
        }
        return count
    }

    /**
     * [ConstantProvider] refuses lookups and writes until something has been loaded into it. A build
     * driven purely from the cache (no RSCM or gameval files) starts with an empty in-memory provider
     * so the collected gamevals still have somewhere to go.
     */
    private fun ensureProvider() {
        if (ConstantProvider.loadedProviders().isNotEmpty()) return
        ConstantProvider.load(BuildMappingProvider())
    }

    /** Empty provider that only ever holds what the build publishes into it. */
    private class BuildMappingProvider : MappingProvider {
        override val mappings: MutableMap<String, MutableMap<String, Int>> =
            mutableMapOf(COMPONENT_TABLE to mutableMapOf())

        override fun load(vararg mappings: File) = Unit

        override fun getSupportedExtensions(): List<String> = emptyList()
    }
}
