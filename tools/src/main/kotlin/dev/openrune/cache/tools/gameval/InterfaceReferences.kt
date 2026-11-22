package dev.openrune.cache.tools.gameval

import com.github.michaelbull.logging.InlineLogger
import dev.openrune.cache.INTERFACES
import dev.openrune.cache.filestore.definition.ComponentDecoder
import dev.openrune.cache.tools.iftype.toml.indexCs2ParamTypes
import dev.openrune.cache.tools.incremental.ConfigKey
import dev.openrune.cache.tools.incremental.ConfigRef
import dev.openrune.cache.tools.incremental.PackState
import dev.openrune.cache.tools.progress.CacheProgress
import dev.openrune.definition.type.widget.ComponentType
import dev.openrune.definition.util.toArray
import dev.openrune.filesystem.Cache
import io.netty.buffer.Unpooled
import java.io.File
import java.util.zip.CRC32

/**
 * The interface half of the gameval reference index: hook arguments on components.
 *
 * A hook (`onLoad`, `onOp`, ...) is a script id followed by its arguments, and the script's source
 * header says what each argument is: `[proc,bankmain_build](component $c0, ...)`. Every argument
 * whose declared type is a gameval table is recorded by name, exactly like a typed config value, so
 * a base component that passes `bankmain:previous_page` to its script is re-encoded when that
 * component's id moves. Types come from the CS2 project, so this half only runs when the build has
 * one; the config half never depends on it.
 *
 * Records share the config tables under the [KIND] kind, keyed by packed component id.
 */
internal object InterfaceReferences {

    private val logger = InlineLogger()

    const val KIND = "IFTYPE"

    private const val META_INDEXED = "gvref.indexed.iftypes"

    /** Hook arguments at or below this are self-reference placeholders or client `event_*` constants. */
    private const val LOWEST_REAL_ARGUMENT = -0x100000 + 1

    /** CS2 parameter types whose gameval table is not simply the type name. */
    private val TABLE_BY_TYPE = mapOf(
        "namedobj" to "obj",
        "toplevelinterface" to "interface",
        "overlayinterface" to "interface",
        "clientinterface" to "interface",
        "graphic" to "sprites",
        "sprite" to "sprites",
        "animation" to "seq",
        "coord" to null,
        "int" to null,
        "boolean" to null,
        "string" to null,
        "char" to null,
    )

    private fun tableFor(type: String, knownTables: Collection<String>): String? {
        if (TABLE_BY_TYPE.containsKey(type)) return TABLE_BY_TYPE[type]
        return type.takeIf { it in knownTables }
    }

    /** Script id -> declared parameter types, from `clientscript.sym` plus the script sources. */
    class ScriptTypes(private val byId: Map<Int, List<String>>) {
        operator fun get(scriptId: Int): List<String>? = byId[scriptId]
        val isEmpty: Boolean get() = byId.isEmpty()
    }

    fun loadScriptTypes(cs2Dir: File): ScriptTypes {
        val idsByName = HashMap<String, Int>()
        for (symbols in listOf(File(cs2Dir, "symbols"), File(cs2Dir, "symbols_custom"))) {
            val sym = File(symbols, "clientscript.sym")
            if (!sym.isFile) continue
            sym.forEachLine { line ->
                val parts = line.split('\t')
                if (parts.size < 2) return@forEachLine
                val id = parts[0].trim().toIntOrNull() ?: return@forEachLine
                // "[proc,name]" -> "name"
                val name = parts[1].trim().substringAfter(',').removeSuffix("]")
                idsByName[name] = id
            }
        }
        if (idsByName.isEmpty()) return ScriptTypes(emptyMap())
        val typesByName = indexCs2ParamTypes(cs2Dir)
        val byId = HashMap<Int, List<String>>(typesByName.size)
        for ((name, types) in typesByName) {
            val id = idsByName[name] ?: continue
            byId[id] = types
        }
        return ScriptTypes(byId)
    }

    // ---- slots ----

    private class HookSlot(val id: String, val table: String, val value: Int, val set: (Int) -> Unit)

    private class Hooks(component: ComponentType) {
        val arrays: LinkedHashMap<String, Array<Any>?> = linkedMapOf(
            "onLoad" to component.onLoad?.copyOf(),
            "onMouseOver" to component.onMouseOver?.copyOf(),
            "onMouseLeave" to component.onMouseLeave?.copyOf(),
            "onTargetLeave" to component.onTargetLeave?.copyOf(),
            "onTargetEnter" to component.onTargetEnter?.copyOf(),
            "onVarTransmit" to component.onVarTransmit?.copyOf(),
            "onInvTransmit" to component.onInvTransmit?.copyOf(),
            "onStatTransmit" to component.onStatTransmit?.copyOf(),
            "onTimer" to component.onTimer?.copyOf(),
            "onOp" to component.onOp?.copyOf(),
            "onMouseRepeat" to component.onMouseRepeat?.copyOf(),
            "onClick" to component.onClick?.copyOf(),
            "onClickRepeat" to component.onClickRepeat?.copyOf(),
            "onRelease" to component.onRelease?.copyOf(),
            "onHold" to component.onHold?.copyOf(),
            "onDrag" to component.onDrag?.copyOf(),
            "onDragComplete" to component.onDragComplete?.copyOf(),
            "onScrollWheel" to component.onScrollWheel?.copyOf(),
        )

        fun apply(component: ComponentType): ComponentType = component.copy(
            onLoad = arrays["onLoad"],
            onMouseOver = arrays["onMouseOver"],
            onMouseLeave = arrays["onMouseLeave"],
            onTargetLeave = arrays["onTargetLeave"],
            onTargetEnter = arrays["onTargetEnter"],
            onVarTransmit = arrays["onVarTransmit"],
            onInvTransmit = arrays["onInvTransmit"],
            onStatTransmit = arrays["onStatTransmit"],
            onTimer = arrays["onTimer"],
            onOp = arrays["onOp"],
            onMouseRepeat = arrays["onMouseRepeat"],
            onClick = arrays["onClick"],
            onClickRepeat = arrays["onClickRepeat"],
            onRelease = arrays["onRelease"],
            onHold = arrays["onHold"],
            onDrag = arrays["onDrag"],
            onDragComplete = arrays["onDragComplete"],
            onScrollWheel = arrays["onScrollWheel"],
        )
    }

    private fun slots(hooks: Hooks, scripts: ScriptTypes, knownTables: Collection<String>): List<HookSlot> {
        val slots = ArrayList<HookSlot>()
        for ((hookName, array) in hooks.arrays) {
            if (array == null || array.size < 2) continue
            val scriptId = array[0] as? Int ?: continue
            val types = scripts[scriptId] ?: continue
            for (i in 1 until array.size) {
                val value = array[i] as? Int ?: continue
                if (value < LOWEST_REAL_ARGUMENT) continue
                val type = types.getOrNull(i - 1) ?: continue
                val table = tableFor(type, knownTables) ?: continue
                slots += HookSlot("$hookName:$i", table, value) { array[i] = it }
            }
        }
        return slots
    }

    private fun extract(
        component: ComponentType,
        scripts: ScriptTypes,
        names: GameValNames,
        knownTables: Collection<String>,
    ): List<ConfigRef> =
        slots(Hooks(component), scripts, knownTables).mapNotNull { slot ->
            val name = names.nameOf(slot.table, slot.value) ?: return@mapNotNull null
            ConfigRef(slot.id, slot.table, name, slot.value)
        }

    // ---- index / relink ----

    /**
     * Brings the interface records up to date. The first time (or after a state reset) every component
     * in the cache is indexed; afterwards only the interfaces in [repackedInterfaces] are swept for new
     * components, while every existing record is checked and relinked as configs are.
     */
    fun relink(
        cache: Cache,
        revision: Int,
        store: PackState,
        cs2Dir: File?,
        repackedInterfaces: Set<Int>,
        progress: CacheProgress,
        stored: Map<ConfigKey, StoredRefsLike>,
    ): Int {
        if (cs2Dir == null || !cs2Dir.isDirectory) return 0
        val scripts = loadScriptTypes(cs2Dir)
        if (scripts.isEmpty) {
            logger.debug { "No script parameter types found under $cs2Dir; interface hooks are not indexed" }
            return 0
        }

        val decoder = ComponentDecoder(cache, revision)
        val names by lazy { GameValReferences.namesFromProvider() }
        val knownTables by lazy { GameValReferences.knownTables(names) }

        val removed = ArrayList<ConfigKey>()
        var relinked = 0
        val relinkedNames = LinkedHashSet<String>()

        fun record(key: ConfigKey, bytes: ByteArray): Boolean {
            val component = runCatching { decoder.read(key.id, Unpooled.wrappedBuffer(bytes), "") }.getOrNull() ?: return false
            val refs = extract(component, scripts, names, knownTables)
            if (refs.isEmpty()) return false
            store.saveConfigRefs(key, crc(bytes), refs)
            return true
        }

        // Existing records: dropped, re-read or relinked.
        for ((key, existing) in stored) {
            val bytes = cache.data(INTERFACES, key.id ushr 16, key.id and 0xFFFF)
            if (bytes == null) {
                removed += key
                continue
            }
            if (crc(bytes) != existing.crc) {
                if (!record(key, bytes)) removed += key
                continue
            }
            val moved = existing.refs.filter { ref ->
                val current = GameValReferences.currentId(ref.table, ref.name)
                current != null && current != ref.id
            }
            if (moved.isEmpty()) continue

            val decoded = runCatching { decoder.read(key.id, Unpooled.wrappedBuffer(bytes), "") }
                .onFailure { failure ->
                    logger.warn(failure) { "Could not decode component ${key.id ushr 16}:${key.id and 0xFFFF} to relink its hooks" }
                }
                .getOrNull()
            if (decoded == null) continue
            val component = decoded
            val hooks = Hooks(component)
            val bySlot = slots(hooks, scripts, knownTables).associateBy { it.id }
            var changed = false
            val updated = existing.refs.map { ref ->
                val slot = bySlot[ref.slot] ?: return@map ref
                val current = GameValReferences.currentId(ref.table, ref.name) ?: return@map ref
                if (current == slot.value) return@map ref.copy(id = current)
                slot.set(current)
                changed = true
                ref.copy(id = current)
            }
            if (!changed) continue

            val writer = Unpooled.buffer(1024)
            decoder.encode(hooks.apply(component), writer)
            val encoded = writer.toArray()
            cache.write(INTERFACES, key.id ushr 16, key.id and 0xFFFF, encoded)
            store.saveConfigRefs(key, crc(encoded), updated)
            relinked++
            relinkedNames += moved.map { "${it.table}.${it.name}" }
        }
        store.deleteConfigRefs(removed)

        // Components not yet on record: everything on the first pass, else only repacked interfaces.
        val firstPass = store.meta(META_INDEXED) == null
        val sweep = if (firstPass) cache.archives(INTERFACES).toList() else repackedInterfaces.toList()
        if (sweep.isNotEmpty()) {
            val bar = if (firstPass) progress.begin("Indexing interface hooks", sweep.size.toLong()) else null
            var indexed = 0
            for (interfaceId in sweep) {
                for (file in cache.files(INTERFACES, interfaceId)) {
                    val key = ConfigKey(KIND, (interfaceId shl 16) or file)
                    if (key in stored) continue
                    val bytes = cache.data(INTERFACES, interfaceId, file) ?: continue
                    if (record(key, bytes)) indexed++
                }
                bar?.step()
            }
            bar?.close()
            if (firstPass) store.putMeta(META_INDEXED, "1")
            logger.debug { "Indexed hook references in $indexed component(s) across ${sweep.size} interface(s)" }
        }

        if (relinked > 0) {
            val sample = relinkedNames.take(5).joinToString()
            val more = if (relinkedNames.size > 5) ", +${relinkedNames.size - 5} more" else ""
            logger.info { "Relinked $relinked component(s) to renumbered gamevals: $sample$more" }
        }
        return relinked
    }

    /** What [relink] needs from a stored record; matches `StoredConfigRefs`. */
    interface StoredRefsLike {
        val crc: Int
        val refs: List<ConfigRef>
    }

    private fun crc(bytes: ByteArray): Int = CRC32().apply { update(bytes) }.value.toInt()
}
