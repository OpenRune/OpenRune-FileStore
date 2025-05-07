package dev.openrune.cache.tools.iftype

import com.displee.cache.CacheLibrary
import com.displee.cache.index.archive.Archive
import dev.openrune.cache.CacheDelegate
import dev.openrune.cache.INTERFACES
import dev.openrune.cache.tools.CacheTool
import dev.openrune.cache.tools.tasks.CacheTask
import dev.openrune.cache.util.getFiles
import dev.openrune.cache.util.progress
import dev.openrune.definition.Js5GameValGroup
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
        var data = ""

        components.forEachIndexed { index, componentType ->
            if (componentType.id != -1) {
                if (componentType.debugInterfaceName != "") {
                    data += "${componentType.debugInterfaceName}:${componentType.id}["
                }

                if (index == 0) {
                    componentType.name = "universe"
                }
            }

            componentType.id = id
            componentType.child = index

            val writer = Unpooled.buffer(4096)
            with(codec) { writer.encode(componentType) }
            data += "${componentType.name}:${index},"
            archive.add(index,writer.toArray())
        }
        data += "]"
        CacheTool.addGameValMapping(Js5GameValGroup.IFTYPES, data.lowercase(), id)
        cache.index(INTERFACES).add(archive)
        cache.update()
    }

}