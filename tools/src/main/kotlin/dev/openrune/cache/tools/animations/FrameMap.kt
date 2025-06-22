package dev.openrune.cache.tools.animations

import io.netty.buffer.ByteBuf

class FrameMap(
    val types: IntArray,
    val labels: Array<IntArray>,
) {
    companion object {
        fun decode(data: ByteBuf): FrameMap {
            val length = data.readUnsignedByte().toInt()
            val types =
                IntArray(length) {
                    data.readUnsignedByte().toInt()
                }
            val labels =
                Array(length) {
                    IntArray(data.readUnsignedByte().toInt())
                }

            labels.forEach { frameMap ->
                for (i in frameMap.indices) {
                    frameMap[i] = data.readUnsignedByte().toInt()
                }
            }
            return FrameMap(types, labels)
        }
    }
}