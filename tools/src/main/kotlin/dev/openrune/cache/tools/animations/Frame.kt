package dev.openrune.cache.tools.animations

import dev.openrune.definition.util.readShortSmart
import io.netty.buffer.ByteBuf

class Frame(
    val frameMapId: Int,
    val translateX: IntArray,
    val translateY: IntArray,
    val translateZ: IntArray,
    val translateCount: Int,
    val indices: IntArray,
    val showing: Boolean,
) {
    companion object {
        fun decodePartial(data: ByteBuf): Frame {
            data.readByte()
            val frameMapId = data.readUnsignedShort()
            return Frame(frameMapId, intArrayOf(), intArrayOf(), intArrayOf(), 0, intArrayOf(), false)
        }

        fun decode(
            frameMaps: Map<Int, FrameMap>,
            data: ByteBuf,
        ): Frame {
            val data0 = data.duplicate()
            val frameMapId = data.readUnsignedShort()
            val frameMap = frameMaps[frameMapId]!!
            val length = data.readUnsignedByte().toInt()
            data0.skipBytes(3 + length)
            val indices = IntArray(500)
            val translatorX = IntArray(500)
            val translatorY = IntArray(500)
            val translatorZ = IntArray(500)

            var lastI = -1
            var index = 0
            var showing = false
            for (i in 0 until length) {
                val opcode = data.readUnsignedByte().toInt()
                if (opcode <= 0) {
                    continue
                }
                if (frameMap.types[i] != 0) {
                    for (j in i - 1 downTo lastI + 1) {
                        if (frameMap.types[j] == 0) {
                            indices[index] = j
                            translatorX[index] = 0
                            translatorY[index] = 0
                            translatorZ[index] = 0
                            index++
                            break
                        }
                    }
                }
                indices[index] = i
                var var11 = 0
                if (frameMap.types[i] == 3) {
                    var11 = 128
                }

                if (opcode and 1 != 0) {
                    translatorX[index] = data0.readShortSmart()
                } else {
                    translatorX[index] = var11
                }

                if (opcode and 2 != 0) {
                    translatorY[index] = data0.readShortSmart()
                } else {
                    translatorY[index] = var11
                }

                if (opcode and 4 != 0) {
                    translatorZ[index] = data0.readShortSmart()
                } else {
                    translatorZ[index] = var11
                }
                if (frameMap.types[i] == 5) {
                    showing = true
                }
                lastI = i
                index++
            }
            return Frame(frameMapId, translatorX, translatorY, translatorZ, length, indices, showing)
        }
    }

    override fun toString(): String {
        return "Frame(frameMapId=$frameMapId)"
    }
}