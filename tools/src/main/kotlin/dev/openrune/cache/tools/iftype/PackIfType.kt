package dev.openrune.cache.tools.iftype

import com.displee.cache.index.archive.Archive
import dev.openrune.cache.INTERFACES
import dev.openrune.cache.filestore.definition.ComponentDecoder
import dev.openrune.cache.filestore.definition.InterfaceType
import dev.openrune.cache.gameval.GameValHandler
import dev.openrune.cache.gameval.impl.Interface
import dev.openrune.cache.tools.CacheTool
import dev.openrune.cache.tools.tasks.CacheTask
import dev.openrune.cache.util.progress
import dev.openrune.definition.GameValGroupTypes
import dev.openrune.definition.util.toArray
import dev.openrune.filesystem.Cache
import io.netty.buffer.Unpooled

class PackIfType(
    private val interfaces : List<InterfaceType>
) : CacheTask() {
    override fun init(cache: Cache) {
        val modelSize = interfaces.size
        val progressInterfaces = progress("Packing iftype's", modelSize)
        interfaces.forEach {
            packInterface(cache,it)
        }

        progressInterfaces.close()

    }

    private fun packInterface(cache: Cache, inf: InterfaceType) {
        val codec = ComponentDecoder(cache)
        val archive = Archive(inf.id)

        val components = emptyList<Interface.InterfaceComponent>().toMutableList()

        inf.components.forEach { (_, component) ->
            val writer = Unpooled.buffer(4096)
            codec.encode(component,writer)
            components.add(Interface.InterfaceComponent(component.internalName?: "com_${component.component}",component.component,component.interfaceId))
            archive.add(component.component,writer.toArray())
        }

        //CacheTool.addGameValMapping(GameValGroupTypes.IFTYPES, Interface(inf.internalName,inf.id,components))
        cache.write(INTERFACES,inf.id,archive.write())
        cache.update()
    }

}