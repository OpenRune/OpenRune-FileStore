package dev.openrune.cache.tools.iftype

import com.displee.cache.CacheLibrary
import com.displee.cache.index.archive.Archive
import dev.openrune.cache.CacheDelegate
import dev.openrune.cache.INTERFACES
import dev.openrune.cache.gameval.impl.Interface
import dev.openrune.cache.tools.CacheTool
import dev.openrune.cache.tools.tasks.CacheTask
import dev.openrune.cache.util.progress
import dev.openrune.definition.GameValGroupTypes
import dev.openrune.definition.codec.ComponentCodec
import dev.openrune.definition.type.widget.ComponentType
import dev.openrune.definition.util.toArray
import dev.openrune.filesystem.Cache
import io.netty.buffer.Unpooled


class PackIfType(
    private val interfaces : List<List<ComponentType>>
) : CacheTask() {
    override fun init(cache: Cache) {
        val modelSize = interfaces.size
        val library = (cache as CacheDelegate).library
        val progressInterfaces = progress("Packing iftype's", modelSize)
        interfaces.forEach {
            packInterface(library,it.first().id,it)
        }

        progressInterfaces.close()

    }

    private fun packInterface(cache: CacheLibrary, id: Int, components: List<ComponentType>) {
        val codec = ComponentCodec()
        val archive = Archive(id)

        var infVal : Interface? = null

        val componentList = emptyList<Interface.InterfaceComponent>().toMutableList()
        var interfaceName = ""
        var interfaceID : Int = -1

        components.forEachIndexed { index, componentType ->
            val components = emptyList<Interface.InterfaceComponent>().toMutableList()
            if (componentType.id != -1) {
                if (componentType.debugInterfaceName != "") {
                    interfaceName = componentType.debugInterfaceName
                    interfaceID = componentType.id
                }

                if (index == 0) {
                    componentType.name = "universe"
                }
            }

            componentType.id = id
            componentType.child = index

            val writer = Unpooled.buffer(4096)
            with(codec) { writer.encode(componentType) }
            components.add(Interface.InterfaceComponent(componentType.name?: "com_${index}",index,componentType.id))
            archive.add(index,writer.toArray())
        }

        CacheTool.addGameValMapping(GameValGroupTypes.IFTYPES, Interface(interfaceName,interfaceID,componentList))
        cache.index(INTERFACES).add(archive)
        cache.update()
    }

}