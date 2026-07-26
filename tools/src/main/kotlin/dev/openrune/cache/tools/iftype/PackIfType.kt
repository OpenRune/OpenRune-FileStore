package dev.openrune.cache.tools.iftype

import com.displee.cache.CacheLibrary
import com.displee.cache.index.archive.Archive
import com.github.michaelbull.logging.InlineLogger
import dev.openrune.cache.CacheDelegate
import dev.openrune.cache.INTERFACES
import dev.openrune.cache.filestore.definition.ComponentDecoder
import dev.openrune.cache.filestore.definition.InterfaceType
import dev.openrune.cache.gameval.GameValHandler
import dev.openrune.cache.gameval.GameValHandler.lookup
import dev.openrune.cache.gameval.GameValHandler.lookupAs
import dev.openrune.cache.gameval.impl.Interface
import dev.openrune.cache.tools.CacheTool
import dev.openrune.cache.tools.iftype.dsl.EditComponent
import dev.openrune.cache.tools.iftype.dsl.InterfaceEdits
import dev.openrune.cache.tools.iftype.dsl.InterfaceFrom
import dev.openrune.cache.tools.iftype.dsl.InterfaceInherit
import dev.openrune.cache.tools.tasks.CacheTask
import dev.openrune.cache.util.progress
import dev.openrune.definition.GameValGroupTypes
import dev.openrune.definition.type.widget.ComponentType
import dev.openrune.definition.util.toArray
import dev.openrune.filesystem.Cache
import io.netty.buffer.Unpooled

class PackIfType(
    private val interfaces: List<InterfaceType>,
) : CacheTask() {
    private val logger = InlineLogger()

    /** Captured once at construction — BUILD packs client then server with the same task instance. */
    private val inheritById =
        interfaces.associate { it.id to InterfaceInherit.take(it.id) }
    private val editsById =
        interfaces.associate { it.id to InterfaceEdits.take(it.id) }
    private val fromById =
        interfaces.associate { it.id to InterfaceFrom.take(it.id) }

    override fun init(cache: Cache) {
        val totalInterfaces = interfaces.size
        val progressInterfaces = progress("Packing iftype's", totalInterfaces)
        val library = (cache as CacheDelegate).library
        val decoder = ComponentDecoder(cache, revision)
        interfaces.forEach {
            val toPack = resolveInherit(cache, decoder, it)
            packInterface(cache, library, decoder, toPack)
            progressInterfaces.step()
        }

        progressInterfaces.close()
    }

    private fun resolveInherit(
        cache: Cache,
        decoder: ComponentDecoder,
        overlay: InterfaceType,
    ): InterfaceType {
        val inheritName = inheritById[overlay.id]
        val edits = editsById[overlay.id].orEmpty()
        val fromByName = fromById[overlay.id].orEmpty()
        if (inheritName == null) {
            if (edits.isNotEmpty() || fromByName.isNotEmpty()) {
                logger.warn { "edit()/from() without inherit() for ${overlay.internalName} — ignored" }
            }
            return overlay
        }
        val base = loadInterface(cache, decoder, inheritName) ?: run {
            logger.warn { "inherit($inheritName) failed for ${overlay.internalName} — packing overlay only" }
            return overlay
        }
        if (base.components.size <= overlay.components.size) {
            logger.warn {
                "inherit($inheritName) base for ${overlay.internalName} only has ${base.components.size} " +
                    "children (cache may already be overwritten) — restore vanilla interface before packing"
            }
        }
        return mergeInherited(base, overlay, edits, fromByName)
    }

    private fun loadInterface(
        cache: Cache,
        decoder: ComponentDecoder,
        internalName: String,
    ): InterfaceType? {
        val gamevals = GameValHandler.readGameVal(GameValGroupTypes.IFTYPES, cache)
        val shortName = internalName.removePrefix("interface.")
        val id =
            gamevals.firstOrNull { it.name == shortName }?.id
                ?: return null
        val files = cache.files(INTERFACES, id)
        if (files.isEmpty()) {
            return null
        }
        val types = mutableMapOf<Int, ComponentType>()
        for (file in files) {
            val combinedId = (id shl 16) or file
            val data = cache.data(INTERFACES, id, file) ?: continue
            val name =
                gamevals.lookupAs<Interface>(id)?.components?.lookup(file)?.name ?: "com_$file"
            types[file] = decoder.read(combinedId, Unpooled.wrappedBuffer(data), name)
        }
        val ifName = gamevals.lookup(id)?.name ?: shortName
        return InterfaceType(types, id, ifName)
    }

    private fun mergeInherited(
        base: InterfaceType,
        overlay: InterfaceType,
        edits: List<EditComponent>,
        fromByName: Map<String, String>,
    ): InterfaceType {
        val result = base.components.toMutableMap()
        val byName =
            result.values
                .mapNotNull { comp -> comp.internalName?.let { it to comp.component } }
                .toMap()
                .toMutableMap()

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
                result[newIndex] = added
                byName[name] = newIndex
            }
        }

        return InterfaceType(result, base.id, base.internalName)
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

        val components = emptyList<Interface.InterfaceComponent>().toMutableList()

        inf.components.toList().sortedBy { (_, component) -> component.component }.forEach { (_, component) ->
            val writer = Unpooled.buffer(4096)
            codec.encode(component, writer)
            components.add(
                Interface.InterfaceComponent(
                    component.internalName ?: "com_${component.component}",
                    component.component,
                    component.interfaceId,
                ),
            )

            archive.add(component.component, writer.toArray())
        }

        CacheTool.addGameValMapping(GameValGroupTypes.IFTYPES, Interface(inf.internalName, inf.id, components))
        cacheLibrary.index(INTERFACES).add(archive)
        cacheLibrary.update()
    }
}
