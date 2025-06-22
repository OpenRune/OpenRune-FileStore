package dev.openrune.cache.tools.animations

import dev.openrune.filesystem.Cache
import io.netty.buffer.Unpooled

class FrameArchive(
    val frames: Map<Int, Frame>,
) {
    companion object {
        const val id: Int = 0

        fun load(
            frameMaps: Map<Int, FrameMap>,
            cache: Cache,
        ): FrameArchive {
            val frames = mutableMapOf<Int, Frame>()
            cache.archives(id).forEach { group ->
                cache.files(id,group).forEach {  file ->
                    val buf = Unpooled.wrappedBuffer(cache.data(id, group, file))
                    val frame =
                        try {
                            Frame.decode(frameMaps, buf)
                        } catch (e: Exception) {
                            buf.readerIndex(0)
                            Frame.decodePartial(buf)
                        } finally {
                            buf.release()
                        }
                    frames[(group shl 16) or file] = frame
                }
            }
            return FrameArchive(frames)
        }

        fun loadPartial(cache: Cache): FrameArchive {
            val frames = mutableMapOf<Int, Frame>()
            cache.archives(id).forEach { group ->
                cache.files(id, group).forEach { file ->
                    val buf = Unpooled.wrappedBuffer(cache.data(id, group, file))
                    val frame = Frame.decodePartial(buf)
                    buf.release()
                    frames[(group shl 16) or file] = frame
                }
            }
            return FrameArchive(frames)
        }
    }
}