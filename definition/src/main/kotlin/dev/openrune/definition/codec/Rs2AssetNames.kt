package dev.openrune.definition.codec

import dev.openrune.cache.gameval.GameValElement
import dev.openrune.definition.Rs2AssetNameGroup
import dev.openrune.definition.util.readString
import dev.openrune.definition.util.writeString
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled

object Rs2AssetNames {

    private const val PACK_TYPE_INDEXED = 1
    private const val PACK_TYPE_KEYED = 2

    data class Entry(val id: Int, val name: String?)

    data class Names(
        val packType: Int,
        val entries: List<Entry>,
    ) {
        val elements: List<GameValElement>
            get() = entries.mapNotNull { row -> row.name?.let { GameValElement(it, row.id) } }

        val unnamed: List<Int> get() = entries.filter { it.name == null }.map { it.id }
    }

    fun decode(buffer: ByteBuf): Names {
        val packType = buffer.readInt()
        require(packType == PACK_TYPE_INDEXED || packType == PACK_TYPE_KEYED) {
            "Unknown RS3 asset name pack type: $packType"
        }

        val count = buffer.readInt()
        val ids = IntArray(count)
        val offsets = IntArray(count)
        for (i in 0 until count) {
            ids[i] = if (packType == PACK_TYPE_INDEXED) i else buffer.readInt()
            offsets[i] = buffer.readInt()
        }

        val tableStart = buffer.readerIndex()
        val entries = ArrayList<Entry>(count)
        for (i in 0 until count) {
            if (offsets[i] == -1) {
                entries += Entry(ids[i], null)
                continue
            }
            buffer.readerIndex(tableStart + offsets[i])
            entries += Entry(ids[i], buffer.readString().lowercase())
        }
        return Names(packType, entries)
    }

    fun encode(names: Names): ByteArray {
        val heap = Unpooled.buffer()
        val out = Unpooled.buffer()
        try {
            out.writeInt(names.packType)
            out.writeInt(names.entries.size)

            val offsets = names.entries.map { entry ->
                val name = entry.name
                if (name == null) {
                    -1
                } else {
                    val at = heap.writerIndex()
                    heap.writeString(name)
                    at
                }
            }

            names.entries.forEachIndexed { index, entry ->
                if (names.packType == PACK_TYPE_KEYED) {
                    out.writeInt(entry.id)
                }
                out.writeInt(offsets[index])
            }
            out.writeBytes(heap)
            return ByteArray(out.readableBytes()).also { out.readBytes(it) }
        } finally {
            heap.release()
            out.release()
        }
    }

    fun splitVarNames(group: Rs2AssetNameGroup, names: Names): Pair<List<GameValElement>, List<GameValElement>> {
        if (!group.hasVarBitNames) return names.elements to emptyList()
        val vars = ArrayList<GameValElement>()
        val bits = ArrayList<GameValElement>()
        var lastNoPrefix = -1
        for (entry in names.entries) {
            val name = entry.name ?: continue
            if (name.startsWith("_")) {
                bits += GameValElement(name.removePrefix("_"), entry.id - lastNoPrefix - 1)
            } else {
                lastNoPrefix = entry.id
                vars += GameValElement(name, entry.id)
            }
        }
        return vars to bits
    }
}
