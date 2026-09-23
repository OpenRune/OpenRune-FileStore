package dev.openrune.cache.tools.iftype

import com.displee.cache.CacheLibrary
import com.displee.cache.index.archive.Archive
import com.github.michaelbull.logging.InlineLogger
import dev.openrune.cache.CacheDelegate
import dev.openrune.cache.INTERFACES
import dev.openrune.cache.filestore.definition.ComponentDecoder
import dev.openrune.cache.filestore.definition.InterfaceType
import dev.openrune.cache.gameval.GameValHandler.lookup
import dev.openrune.cache.gameval.GameValHandler.lookupAs
import dev.openrune.cache.gameval.impl.Interface
import dev.openrune.cache.tools.CacheTool
import dev.openrune.cache.tools.TaskPriority
import dev.openrune.cache.tools.iftype.dsl.EditComponent
import dev.openrune.cache.tools.iftype.dsl.InterfaceEdits
import dev.openrune.cache.tools.iftype.dsl.InterfaceFrom
import dev.openrune.cache.tools.iftype.dsl.InterfaceInherit
import dev.openrune.cache.tools.iftype.dsl.InterfaceParents
import dev.openrune.cache.tools.iftype.dsl.InterfacePlacements
import dev.openrune.cache.tools.iftype.dsl.Placement
import dev.openrune.cache.tools.iftype.dsl.SelfRef
import dev.openrune.cache.tools.iftype.toml.indexCs2ParamTypes
import dev.openrune.cache.tools.iftype.toml.loadInterfaceToml
import dev.openrune.cache.tools.tasks.CacheTask
import dev.openrune.cache.util.getFiles
import dev.openrune.cache.util.progress
import dev.openrune.definition.GameValGroupTypes
import dev.openrune.definition.constants.ConstantProvider
import dev.openrune.definition.type.widget.ComponentType
import dev.openrune.definition.util.toArray
import dev.openrune.filesystem.Cache
import dev.openrune.toml.rsconfig.rsconfig
import dev.openrune.toml.tomlMapper
import io.netty.buffer.Unpooled
import java.io.File
import java.nio.file.Path

class PackIfType(
    interfaces: List<InterfaceType> = emptyList(),
    tomlDirectories: List<File> = emptyList(),
    cs2Directory: File? = null,
    tokenizedReplacements: Map<String, String> = emptyMap(),
    tokenizedFile: Path? = null,
) : CacheTask() {
    private val logger = InlineLogger()

    private val interfaces: List<InterfaceType> = run {
        val cs2ParamTypes = cs2Directory?.let { indexCs2ParamTypes(it) }.orEmpty()
        interfaces + tomlDirectories.flatMap { resolveTomlInterfaces(it, cs2ParamTypes, tokenizedReplacements, tokenizedFile) }
    }

    // `this.` is load-bearing: the constructor parameter of the same name shadows the property here.
    private val inheritById =
        this.interfaces.associate { it.id to InterfaceInherit.take(it.id) }
    private val editsById =
        this.interfaces.associate { it.id to InterfaceEdits.take(it.id) }
    private val fromById =
        this.interfaces.associate { it.id to InterfaceFrom.take(it.id) }
    private val placementsById =
        this.interfaces.associate { it.id to InterfacePlacements.take(it.id) }
    private val parentsById =
        this.interfaces.associate { it.id to InterfaceParents.take(it.id) }

    /** Interfaces pack before configs so `component.*` ids they define resolve everywhere else. */
    override val priority: TaskPriority
        get() = TaskPriority.INTERFACES

    override fun init(cache: Cache) {
        val totalInterfaces = interfaces.size
        val progressInterfaces = progress.begin("Packing iftype's", totalInterfaces)
        val library = (cache as CacheDelegate).library
        val decoder = ComponentDecoder(cache, revision)
        interfaces.forEach {
            val toPack = resolveInherit(cache, decoder, it)
            packInterface(cache, library, decoder, toPack)
            progressInterfaces.step()
        }

        progressInterfaces.close()
    }

    /**
     * Resolves these interfaces without packing them and feeds the resulting ids into
     * [ConstantProvider], so `component.<interface>:<name>` references decode anywhere in the build.
     *
     * Resolution is deterministic — inherit bases come from [cache] and the overlay directives were
     * captured at construction — so the ids published here are the ones [init] goes on to write.
     * This matters most when `insertAfter`/`insertBefore` renumbers a base interface's children:
     * both the inserted component and the siblings it shifted are only discoverable by performing
     * the merge, and the mappings loaded from the previous build's gamevals are stale for them.
     */
    fun publishComponentIds(cache: Cache, revision: Int) {
        val decoder = ComponentDecoder(cache, revision)
        var published = 0
        interfaces.forEach { interf ->
            val resolved = resolveInherit(cache, decoder, interf)
            ConstantProvider.putMapping(INTERFACE_TABLE, resolved.internalName, resolved.id)
            for ((_, component) in resolved.components) {
                val name = component.internalName ?: continue
                ConstantProvider.putMapping(
                    COMPONENT_TABLE,
                    "${resolved.internalName}:$name",
                    component.packed,
                )
                published++
            }
        }
        logger.debug { "Published $published component ids for ${interfaces.size} interfaces" }
    }

    private fun gameValElement(inf: InterfaceType): Interface {
        val components =
            inf.components
                .toList()
                .sortedBy { (_, component) -> component.component }
                .map { (_, component) ->
                    Interface.InterfaceComponent(
                        component.internalName ?: "com_${component.component}",
                        component.component,
                        component.interfaceId,
                    )
                }
        return Interface(inf.internalName, inf.id, components)
    }

    private fun resolveInherit(
        cache: Cache,
        decoder: ComponentDecoder,
        overlay: InterfaceType,
    ): InterfaceType {
        val inheritName = inheritById[overlay.id]
        val edits = editsById[overlay.id].orEmpty()
        val fromByName = fromById[overlay.id].orEmpty()
        val placements = placementsById[overlay.id].orEmpty()
        val parents = parentsById[overlay.id].orEmpty()
        if (inheritName == null) {
            if (edits.isNotEmpty() || fromByName.isNotEmpty() || placements.isNotEmpty() || parents.isNotEmpty()) {
                logger.warn {
                    "edit()/from()/insertAfter()/parent() without inherit() for ${overlay.internalName} — ignored"
                }
            }
            return overlay
        }
        val base = loadInterface(cache, decoder, inheritName)
            ?: error("inherit($inheritName) failed for ${overlay.internalName}")
        check(base.components.size > overlay.components.size) {
            "inherit($inheritName) base for ${overlay.internalName} only has ${base.components.size} " +
                "children - restore the vanilla interface before packing"
        }
        return mergeInherited(base, overlay, edits, fromByName, placements, parents)
    }

    private fun loadInterface(
        cache: Cache,
        decoder: ComponentDecoder,
        internalName: String,
    ): InterfaceType? {
        // The decoder already read this group at construction.
        val gamevals = decoder.gamevals
        val shortName = internalName.removePrefix("interface.")
        val id =
            gamevals.firstOrNull { it.name == shortName }?.id
                ?: return null
        val files = cache.files(INTERFACES, id)
        if (files.isEmpty()) {
            return null
        }
        val types = mutableMapOf<Int, ComponentType>()
        val gameval = gamevals.lookupAs<Interface>(id)
        for (file in files) {
            val combinedId = (id shl 16) or file
            val data = cache.data(INTERFACES, id, file) ?: continue
            val name = gameval?.component(file)?.name ?: "com_$file"
            types[file] = decoder.read(combinedId, Unpooled.wrappedBuffer(data), name)
        }
        val ifName = gamevals.lookup(id)?.name ?: shortName
        return InterfaceType(types, id, ifName)
    }

    /**
     * Lays the overlay over the cache base: named matches are edited in place, everything else is
     * appended. Parents and hook self-references are resolved by name at the end because an
     * overlay only knows its own numbering, which is meaningless once merged into the base.
     *
     * A component that gives an insert anchor but no parent adopts the anchor's parent, so slotting
     * a component in beside an existing one only takes the anchor's name.
     */
    private fun mergeInherited(
        base: InterfaceType,
        overlay: InterfaceType,
        edits: List<EditComponent>,
        fromByName: Map<String, String>,
        placements: Map<String, Placement>,
        parents: Map<String, String>,
    ): InterfaceType {
        val result = base.components.toMutableMap()
        val byName =
            result.values
                .mapNotNull { comp -> comp.internalName?.let { it to comp.component } }
                .toMap()
                .toMutableMap()

        fun anchorParent(name: String): Int? {
            val anchor = placements[name]?.anchor ?: return null
            val anchorIndex =
                byName[anchor]
                    ?: run {
                        logger.warn {
                            "insert anchor \"$anchor\" for \"$name\" — component not found on " +
                                "${base.internalName}; cannot inherit its parent"
                        }
                        return null
                    }
            return result[anchorIndex]?.layer
        }

        fun resolveParent(name: String, fallback: Int): Int {
            val parentName = parents[name] ?: return anchorParent(name) ?: fallback
            val parentIndex =
                byName[parentName]
                    ?: run {
                        if (parentName != "universe") {
                            logger.warn {
                                "parent(\"$parentName\") for \"$name\" — component not found on " +
                                    "${base.internalName}; attaching to universe"
                            }
                        }
                        0
                    }
            return (base.id shl 16) or parentIndex
        }

        for (edit in edits) {
            val index = byName[edit.name]
            if (index == null) {
                logger.warn { "edit(\"${edit.name}\") — component not found on ${base.internalName}" }
                continue
            }
            result[index] = applyEdit(result.getValue(index), edit)
        }

        for ((_, dsl) in overlay.components.entries.sortedBy { it.key }) {
            val name = dsl.internalName ?: continue
            if (name == "universe") {
                val universe = result[0] ?: continue
                result[0] =
                    universe.copy(
                        width = if (dsl.width > 0) dsl.width else universe.width,
                        height = if (dsl.height > 0) dsl.height else universe.height,
                    )
                continue
            }

            val existingIndex = byName[name]
            if (existingIndex != null) {
                val baseComp = result.getValue(existingIndex)
                result[existingIndex] =
                    baseComp.copy(
                        x = dsl.x,
                        y = dsl.y,
                        width = if (dsl.width > 0) dsl.width else baseComp.width,
                        height = if (dsl.height > 0) dsl.height else baseComp.height,
                        // Layout modes and parent must track the DSL too. Without this an overlay
                        // packed over its own previous output can never correct them, so a component
                        // keeps whatever anchoring it was first created with.
                        xMode = dsl.xMode,
                        yMode = dsl.yMode,
                        widthMode = dsl.widthMode,
                        heightMode = dsl.heightMode,
                        layer = resolveParent(name, if (dsl.layer == -1) baseComp.layer else dsl.layer),
                        op = if (dsl.op.any { it.isNotBlank() }) dsl.op else baseComp.op,
                        events = if (dsl.events != 0) dsl.events else baseComp.events,
                        graphic =
                            if (dsl.type == 5 && dsl.graphic > 0) dsl.graphic else baseComp.graphic,
                        internalId = (base.id shl 16) or existingIndex,
                        internalName = name,
                        id = existingIndex,
                    )
            } else {
                val newIndex = (result.keys.maxOrNull() ?: -1) + 1
                val packed = (base.id shl 16) or newIndex
                val donorName = fromByName[name]
                var added =
                    if (donorName != null) {
                        val donor =
                            result.values.firstOrNull { it.internalName == donorName }
                                ?: run {
                                    logger.warn {
                                        "from(\"$donorName\") for \"$name\" — donor not found; packing DSL only"
                                    }
                                    null
                                }
                        if (donor != null) {
                            cloneFromDonor(donor, dsl, packed, name, newIndex)
                        } else {
                            dsl.copy(
                                internalId = packed,
                                internalName = name,
                                id = newIndex,
                                layer = if (dsl.layer == -1) packed and -65536 else dsl.layer,
                            )
                        }
                    } else {
                        dsl.copy(
                            internalId = packed,
                            internalName = name,
                            id = newIndex,
                            layer = if (dsl.layer == -1) packed and -65536 else dsl.layer,
                        )
                    }
                if (donorName == null) {
                    added = remapHookSelfRefs(added, fromPacked = dsl.packed, toPacked = packed)
                }
                added = added.copy(layer = resolveParent(name, added.layer))
                logger.debug {
                    "merge ${base.internalName}: added \"$name\" at $newIndex " +
                        "donor=${donorName ?: "none"} dsl=${dsl.width}x${dsl.height} " +
                        "added=${added.width}x${added.height} layer=${added.layer}"
                }
                result[newIndex] = added
                byName[name] = newIndex
            }
        }

        val overlayNameByIndex =
            overlay.components.mapNotNull { (index, comp) -> comp.internalName?.let { index to it } }.toMap()
        fun resolveSelfRef(value: Int): Int {
            val overlayIndex = SelfRef.decode(value) ?: return value
            val name = overlayNameByIndex[overlayIndex]
            val merged = name?.let { byName[it] }
            if (merged == null) {
                logger.warn { "self reference #$overlayIndex in ${base.internalName} overlay could not be resolved" }
                return -1
            }
            return (base.id shl 16) or merged
        }
        for ((index, component) in result.toMap()) {
            result[index] = mapHookInts(component, ::resolveSelfRef)
        }

        return InterfaceType(applyPlacements(base.id, result, placements), base.id, base.internalName)
    }

    private fun mapHookInts(component: ComponentType, transform: (Int) -> Int): ComponentType {
        fun remap(hook: Array<Any>?): Array<Any>? {
            if (hook == null) return null
            return Array(hook.size) { i ->
                val value = hook[i]
                if (value is Int) transform(value) else value
            }
        }
        return component.copy(
            onLoad = remap(component.onLoad),
            onMouseOver = remap(component.onMouseOver),
            onMouseLeave = remap(component.onMouseLeave),
            onTargetLeave = remap(component.onTargetLeave),
            onTargetEnter = remap(component.onTargetEnter),
            onVarTransmit = remap(component.onVarTransmit),
            onInvTransmit = remap(component.onInvTransmit),
            onStatTransmit = remap(component.onStatTransmit),
            onTimer = remap(component.onTimer),
            onOp = remap(component.onOp),
            onMouseRepeat = remap(component.onMouseRepeat),
            onClick = remap(component.onClick),
            onClickRepeat = remap(component.onClickRepeat),
            onRelease = remap(component.onRelease),
            onHold = remap(component.onHold),
            onDrag = remap(component.onDrag),
            onDragComplete = remap(component.onDragComplete),
            onScrollWheel = remap(component.onScrollWheel),
        )
    }

    /**
     * Moves each `insertAfter`/`insertBefore` component to its anchor's sibling slot.
     *
     * Draw order among siblings is child-index order, so repositioning means renumbering. The
     * existing (sparse) index set is reused as the slot pool and components are reassigned to it in
     * the new order, which keeps every index before the insertion point untouched and shifts only
     * the tail. Every reference that lives inside this group — `layer` parents and packed ids
     * embedded in hook argument arrays — is rewritten to match. References from *outside* the group
     * (CS2 that hardcodes a packed component id) cannot be seen from here and will still point at
     * the old index.
     */
    private fun applyPlacements(
        interfaceId: Int,
        components: Map<Int, ComponentType>,
        placements: Map<String, Placement>,
    ): Map<Int, ComponentType> {
        if (placements.isEmpty()) return components

        val indexByName =
            components.values
                .mapNotNull { comp -> comp.internalName?.let { it to comp.component } }
                .toMap()

        val slots = components.keys.sorted()
        val order = slots.toMutableList()
        for ((name, placement) in placements) {
            val moving = indexByName[name]
            if (moving == null) {
                logger.warn { "insert(\"$name\") — component not found after merge" }
                continue
            }
            val anchor = indexByName[placement.anchor]
            if (anchor == null) {
                logger.warn { "insert anchor \"${placement.anchor}\" not found for \"$name\"" }
                continue
            }
            if (anchor == moving) {
                logger.warn { "insert anchor for \"$name\" is itself — ignored" }
                continue
            }
            order.remove(moving)
            val at = order.indexOf(anchor)
            order.add(if (placement.before) at else at + 1, moving)
            val comp = components.getValue(moving)
            logger.debug {
                "placement on $interfaceId: \"$name\" from $moving to after \"${placement.anchor}\" " +
                    "($anchor), carrying ${comp.width}x${comp.height}"
            }
        }

        val remap = order.withIndex().associate { (position, old) -> old to slots[position] }
        if (remap.all { (old, new) -> old == new }) return components

        val packedRemap =
            remap.entries.associate { (old, new) ->
                ((interfaceId shl 16) or old) to ((interfaceId shl 16) or new)
            }

        val relocated =
            components.entries.associate { (oldIndex, comp) ->
                val newIndex = remap.getValue(oldIndex)
                newIndex to relocate(comp, newIndex, interfaceId, packedRemap)
            }
        check(relocated.size == components.size) {
            "insert() on interface $interfaceId collapsed ${components.size} components into " +
                "${relocated.size}; the slot remap is not one-to-one and components would be lost"
        }
        return relocated
    }

    private fun relocate(
        component: ComponentType,
        newIndex: Int,
        interfaceId: Int,
        packedRemap: Map<Int, Int>,
    ): ComponentType {
        val packed = (interfaceId shl 16) or newIndex
        fun remapHook(hook: Array<Any>?): Array<Any>? {
            if (hook == null) return null
            return Array(hook.size) { i ->
                val value = hook[i]
                if (value is Int) packedRemap[value] ?: value else value
            }
        }
        return component.copy(
            internalId = packed,
            id = newIndex,
            layer = packedRemap[component.layer] ?: component.layer,
            onLoad = remapHook(component.onLoad),
            onMouseOver = remapHook(component.onMouseOver),
            onMouseLeave = remapHook(component.onMouseLeave),
            onTargetLeave = remapHook(component.onTargetLeave),
            onTargetEnter = remapHook(component.onTargetEnter),
            onVarTransmit = remapHook(component.onVarTransmit),
            onInvTransmit = remapHook(component.onInvTransmit),
            onStatTransmit = remapHook(component.onStatTransmit),
            onTimer = remapHook(component.onTimer),
            onOp = remapHook(component.onOp),
            onMouseRepeat = remapHook(component.onMouseRepeat),
            onClick = remapHook(component.onClick),
            onClickRepeat = remapHook(component.onClickRepeat),
            onRelease = remapHook(component.onRelease),
            onHold = remapHook(component.onHold),
            onDrag = remapHook(component.onDrag),
            onDragComplete = remapHook(component.onDragComplete),
            onScrollWheel = remapHook(component.onScrollWheel),
        )
    }

    private fun cloneFromDonor(
        donor: ComponentType,
        dsl: ComponentType,
        packed: Int,
        name: String,
        newIndex: Int,
    ): ComponentType {
        val cloned =
            donor.copy(
                x = dsl.x,
                y = dsl.y,
                width = if (dsl.width > 0) dsl.width else donor.width,
                height = if (dsl.height > 0) dsl.height else donor.height,
                op = if (dsl.op.any { it.isNotBlank() }) dsl.op else donor.op,
                events = if (dsl.events != 0) dsl.events else donor.events,
                graphic = if (dsl.type == 5 && dsl.graphic > 0) dsl.graphic else donor.graphic,
                internalId = packed,
                internalName = name,
                id = newIndex,
            )
        return remapHookSelfRefs(cloned, fromPacked = donor.packed, toPacked = packed)
    }

    private fun applyEdit(base: ComponentType, edit: EditComponent): ComponentType =
        base.copy(
            x = edit.x ?: base.x,
            y = edit.y ?: base.y,
            width = edit.width ?: base.width,
            height = edit.height ?: base.height,
            graphic = edit.graphic ?: base.graphic,
            hide = edit.hide ?: base.hide,
            scrollWidth = edit.scrollWidth ?: base.scrollWidth,
            scrollHeight = edit.scrollHeight ?: base.scrollHeight,
        )

    private fun remapHookSelfRefs(
        component: ComponentType,
        fromPacked: Int,
        toPacked: Int,
    ): ComponentType {
        fun remap(hook: Array<Any>?): Array<Any>? {
            if (hook == null) return null
            return Array(hook.size) { i ->
                val v = hook[i]
                if (v is Int && v == fromPacked) toPacked else v
            }
        }
        return component.copy(
            onLoad = remap(component.onLoad),
            onMouseOver = remap(component.onMouseOver),
            onMouseLeave = remap(component.onMouseLeave),
            onOp = remap(component.onOp),
            onClick = remap(component.onClick),
            onClickRepeat = remap(component.onClickRepeat),
            onMouseRepeat = remap(component.onMouseRepeat),
        )
    }

    private fun packInterface(
        cache: Cache,
        cacheLibrary: CacheLibrary,
        codec: ComponentDecoder,
        inf: InterfaceType,
    ) {
        val archive = Archive(inf.id)

        inf.components.toList().sortedBy { (_, component) -> component.component }.forEach { (_, component) ->
            val writer = Unpooled.buffer(4096)
            codec.encode(component, writer)
            archive.add(component.component, writer.toArray())
        }

        CacheTool.addGameValMapping(GameValGroupTypes.IFTYPES, gameValElement(inf))
        cacheLibrary.index(INTERFACES).add(archive)
        cacheLibrary.update()
        packedThisBuild += inf.id
    }

    companion object {
        private const val INTERFACE_TABLE = "interface"
        private const val COMPONENT_TABLE = "component"

        /**
         * Interfaces written this build. Interfaces bypass the recording cache, so the gameval reference
         * index reads this to know which ones may have new components to track. Cleared per build.
         */
        val packedThisBuild: MutableSet<Int> = LinkedHashSet()
    }
}

private fun resolveTomlInterfaces(
    directory: File,
    cs2ParamTypes: Map<String, List<String>>,
    tokenizedReplacements: Map<String, String>,
    tokenizedFile: Path?,
): List<InterfaceType> {
    val files = getFiles(directory, "if3")
    if (files.isEmpty()) return emptyList()

    val mapper = tomlMapper {
        rsconfig {
            enableConstantProvider()
            enabledTokenizedReplacement(tokenizedReplacements, tokenizedFile)
        }
    }

    return files.map { file ->
        try {
            loadInterfaceToml(file.toPath(), mapper, cs2ParamTypes)
        } catch (e: Exception) {
            error("Failed to load interface TOML '${file.name}': ${e.message}")
        }
    }
}
