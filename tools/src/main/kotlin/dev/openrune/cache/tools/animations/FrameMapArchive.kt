package dev.openrune.cache.tools.animations

import dev.openrune.filesystem.Cache
import io.netty.buffer.Unpooled

class FrameMapArchive(
    val frameMaps: Map<Int, FrameMap>,
) {
    companion object {
        const val id: Int = 1

        fun load(cache: Cache): FrameMapArchive {
            val frameMaps = mutableMapOf<Int, FrameMap>()
            cache.archives(id).forEach { group ->
                val buf = Unpooled.wrappedBuffer(cache.data(id, group, 0))
                frameMaps[group] = FrameMap.decode(buf)
                buf.release()
            }
            return FrameMapArchive(frameMaps)
        }
    }
}