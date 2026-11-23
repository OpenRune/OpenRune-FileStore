package dev.openrune.definition.codec

import dev.openrune.definition.type.Rs2ModelRender
import dev.openrune.definition.type.Rs2ModelType
import io.netty.buffer.ByteBuf

object Rs2ModelDecoder {

    fun decode(id: Int, buffer: ByteBuf, build: Int): Rs2ModelType {
        val model = Rs2ModelType(id = id)
        val length = buffer.readableBytes()
        val base = buffer.readerIndex()

        model.format = buffer.readUnsignedByte().toInt()
        model.version = buffer.readUnsignedByte().toInt()
        buffer.readUnsignedByte()
        val renderCount = buffer.readUnsignedByte().toInt()
        buffer.readUnsignedByte()
        val billboards = buffer.readUnsignedByte().toInt()
        val emitters = buffer.readUnsignedByte().toInt()
        val effectors = buffer.readUnsignedByte().toInt()
        if (model.version >= 5) buffer.readUnsignedByte()
        check(model.version >= 4) { "Model $id: RT7 version ${model.version} per-mesh layout is not supported" }

        val billboardSize = if (build >= 923) 39 else 37
        val emitterSize = if (build >= 923) 50 else 44
        val effectorSize = if (build >= 923) 18 else 16
        val tail = billboards * billboardSize + emitters * emitterSize + effectors * effectorSize
        val renderEnd = base + length - tail

        decodeShared(model, buffer, renderCount, renderEnd)

        model.billboardCount = billboards
        model.emitterCount = emitters
        model.effectorCount = effectors
        buffer.skipBytes(tail)
        check(!buffer.isReadable) { "Model $id: ${buffer.readableBytes()} trailing bytes" }
        return model
    }

    private fun decodeShared(model: Rs2ModelType, buffer: ByteBuf, renderCount: Int, renderEnd: Int) {
        val flags = buffer.readUnsignedByte().toInt()
        val positionFormat = buffer.readUnsignedByte().toInt()
        buffer.skipBytes(2)
        val vertexCount = buffer.readIntLE()
        model.vertexCount = vertexCount

        if (flags and Rs2ModelType.FLAG_HAS_VERTICES != 0) {
            buffer.skipBytes(vertexCount * 3 * if (positionFormat == 1) 4 else 2)
            buffer.skipBytes(vertexCount * 3)
            buffer.skipBytes(vertexCount * 4)
            buffer.skipBytes(vertexCount * 2)
        }
        if (flags and Rs2ModelType.FLAG_HAS_BONE_IDS != 0) {
            buffer.skipBytes(vertexCount * 2)
        }
        if (flags and Rs2ModelType.FLAG_HAS_SKIN != 0) {
            repeat(vertexCount) {
                val ids = buffer.readUnsignedShortLE()
                buffer.skipBytes(ids * 2)
                val weights = buffer.readUnsignedShortLE()
                buffer.skipBytes(weights)
            }
        }
        if (flags and Rs2ModelType.FLAG_HAS_VERTICES != 0) {
            buffer.skipBytes(vertexCount * 2)
            buffer.skipBytes(vertexCount)
        }
        if (flags and Rs2ModelType.FLAG_HAS_FACE_BONES != 0) {
            buffer.skipBytes(vertexCount * 2)
        }

        val solved = mutableListOf<Rs2ModelRender>()
        val solutions = solveRenders(
            buffer, buffer.readerIndex(), 0, renderCount, vertexCount, renderEnd, mutableListOf(), solved,
        )
        check(solutions == 1) {
            if (solutions == 0) {
                "Model ${model.id}: no consistent index buffer layout"
            } else {
                "Model ${model.id}: ambiguous index buffer layout"
            }
        }
        buffer.readerIndex(renderEnd)
        model.renders = solved
    }

    private fun solveRenders(
        buffer: ByteBuf,
        position: Int,
        render: Int,
        count: Int,
        vertexCount: Int,
        end: Int,
        current: MutableList<Rs2ModelRender>,
        result: MutableList<Rs2ModelRender>,
    ): Int {
        if (render == count) {
            if (position != end) return 0
            if (result.isEmpty()) result += current
            return 1
        }
        if (position + 10 > end) return 0

        val stored = buffer.getUnsignedShortLE(position + 8)
        val size = if (vertexCount <= 0xFFFF) 2 else 4
        val dataStart = position + 10
        var solutions = 0

        var length = stored.toLong() + 65536L * Math.floorMod(-stored, 3)
        while (dataStart + length * size <= end) {
            if (indicesValid(buffer, dataStart, length.toInt(), size, vertexCount)) {
                current += Rs2ModelRender(
                    flags = buffer.getUnsignedByte(position).toInt(),
                    material = buffer.getUnsignedShortLE(position + 5) - 1,
                    lod = buffer.getUnsignedByte(position + 7).toInt(),
                    unknown = buffer.getIntLE(position + 1),
                    indexCount = length.toInt(),
                )
                solutions += solveRenders(
                    buffer, dataStart + length.toInt() * size, render + 1, count, vertexCount, end, current, result,
                )
                current.removeAt(current.size - 1)
                if (solutions > 1) break
            }
            length += 65536L * 3
        }
        return solutions
    }

    private fun indicesValid(buffer: ByteBuf, position: Int, length: Int, size: Int, vertexCount: Int): Boolean {
        for (i in 0 until length) {
            val value = if (size == 2) {
                buffer.getUnsignedShortLE(position + i * 2)
            } else {
                buffer.getInt(position + i * 4)
            }
            if (value < 0 || value >= vertexCount) return false
        }
        return true
    }
}
