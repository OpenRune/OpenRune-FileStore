package dev.openrune.cache.tools.iftype

import com.displee.cache.CacheLibrary
import com.displee.cache.index.archive.Archive
import dev.openrune.cache.CacheDelegate
import dev.openrune.cache.INTERFACES
import dev.openrune.cache.filestore.definition.ComponentDecoder
import dev.openrune.cache.filestore.definition.InterfaceType
import dev.openrune.cache.gameval.impl.Interface
import dev.openrune.cache.tools.CacheTool
import dev.openrune.cache.tools.tasks.CacheTask
import dev.openrune.cache.util.progress
import dev.openrune.definition.GameValGroupTypes
import dev.openrune.definition.type.widget.ComponentType
import dev.openrune.definition.util.toArray
import dev.openrune.filesystem.Cache
import io.netty.buffer.Unpooled
import kotlin.collections.sortedBy

class PackIfType(
    private val interfaces : List<InterfaceType>
) : CacheTask() {
    override fun init(cache: Cache) {
        val totalInterfaces = interfaces.size
        val progressInterfaces = progress("Packing iftype's", totalInterfaces)
        val library = (cache as CacheDelegate).library
        interfaces.forEach {
            packInterface(cache,library,it)
            progressInterfaces.step()
        }

        progressInterfaces.close()

    }

    private fun packInterface(cache: Cache,cacheLibrary: CacheLibrary, inf: InterfaceType) {
        val codec = ComponentDecoder(cache)
        val archive = Archive(inf.id)

        val components = emptyList<Interface.InterfaceComponent>().toMutableList()

        inf.components.toList().sortedBy { (_, component) -> component.component }.forEach { (_, component) ->
            val writer = Unpooled.buffer(4096)
            codec.encode(component, writer)

            components.add(
                Interface.InterfaceComponent(
                    component.internalName ?: "com_${component.component}",
                    component.component,
                    component.interfaceId
                )
            )

            archive.add(component.component, writer.toArray())
        }

        CacheTool.addGameValMapping(GameValGroupTypes.IFTYPES, Interface(inf.internalName,inf.id,components))
        cacheLibrary.index(INTERFACES).add(archive)
        cacheLibrary.update()

        val inf : MutableMap<Int, InterfaceType> = emptyMap<Int, InterfaceType>().toMutableMap()

        codec.load(inf)

        inf.forEach {
            println(it)
        }

        println("Size:eeee ${cache.files(INTERFACES, 1200).size}")

    }

}