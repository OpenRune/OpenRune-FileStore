package dev.openrune.definition.codec

import dev.openrune.definition.type.Rs2NpcOverride
import dev.openrune.definition.type.Rs2OverrideGroup
import dev.openrune.definition.type.Rs2OverrideModel
import dev.openrune.definition.type.Rs2OverrideRecolour
import dev.openrune.definition.type.Rs2OverrideTint
import dev.openrune.definition.util.readSmart2or4
import dev.openrune.definition.util.readUnsignedShortOrNull
import dev.openrune.definition.util.writeSmart2or4
import io.netty.buffer.ByteBuf

internal fun ByteBuf.readNpcOverride(mediumVarBit: Boolean): Rs2NpcOverride {
    val override = Rs2NpcOverride(mediumVarBit = mediumVarBit)
    override.leading = readUnsignedShort()
    override.varBit = if (mediumVarBit) readMedium() else readUnsignedShortOrNull() ?: -1
    override.varPlayer = readUnsignedShortOrNull() ?: -1
    val flags = readUnsignedByte().toInt()
    override.flags = flags

    if ((flags and 1) != 0) {
        override.models = readGroups {
            val from = readUnsignedShort()
            val to = readUnsignedShort()
            val model = readSmart2or4()
            val extraCount = readUnsignedByte().toInt()
            Rs2OverrideModel(from, to, model, MutableList(extraCount) { readUnsignedByte().toInt() })
        }
    }
    if ((flags and 2) != 0) {
        override.headModels = readGroups {
            Rs2OverrideModel(readUnsignedShort(), readUnsignedShort(), readSmart2or4())
        }
    }
    if ((flags and 4) != 0) {
        override.retextures = readGroups {
            Rs2OverrideRecolour(readUnsignedShort(), readUnsignedShort(), readUnsignedShort(), readUnsignedShort())
        }
    }
    if ((flags and 8) != 0) {
        override.recolours = readGroups {
            Rs2OverrideRecolour(readUnsignedShort(), readUnsignedShort(), readUnsignedShort(), readUnsignedShort())
        }
    }
    if ((flags and 16) != 0) {
        val count = readUnsignedByte().toInt()
        override.tints = MutableList(count) {
            Rs2OverrideTint(
                readUnsignedShort(),
                readUnsignedShort(),
                readUnsignedByte().toInt(),
                readUnsignedByte().toInt(),
                readUnsignedByte().toInt(),
                readUnsignedByte().toInt(),
            )
        }
    }
    override.default = readUnsignedShort()
    return override
}

private fun <T> ByteBuf.readGroups(entry: ByteBuf.() -> T): MutableList<Rs2OverrideGroup<T>> {
    val groupCount = readUnsignedByte().toInt()
    return MutableList(groupCount) {
        val key = readUnsignedByte().toInt()
        val entryCount = readUnsignedByte().toInt()
        Rs2OverrideGroup(key, MutableList(entryCount) { entry() })
    }
}

internal fun ByteBuf.writeNpcOverride(override: Rs2NpcOverride) {
    writeShort(override.leading)
    if (override.mediumVarBit) writeMedium(override.varBit) else writeShort(override.varBit)
    writeShort(override.varPlayer)
    writeByte(override.flags)

    if ((override.flags and 1) != 0) {
        writeGroups(override.models) { model ->
            writeShort(model.from)
            writeShort(model.to)
            writeSmart2or4(model.model)
            writeByte(model.extra.size)
            model.extra.forEach { writeByte(it) }
        }
    }
    if ((override.flags and 2) != 0) {
        writeGroups(override.headModels) { model ->
            writeShort(model.from)
            writeShort(model.to)
            writeSmart2or4(model.model)
        }
    }
    if ((override.flags and 4) != 0) {
        writeGroups(override.retextures) { writeRecolour(it) }
    }
    if ((override.flags and 8) != 0) {
        writeGroups(override.recolours) { writeRecolour(it) }
    }
    if ((override.flags and 16) != 0) {
        writeByte(override.tints.size)
        for (tint in override.tints) {
            writeShort(tint.from)
            writeShort(tint.to)
            writeByte(tint.hue)
            writeByte(tint.saturation)
            writeByte(tint.lightness)
            writeByte(tint.opacity)
        }
    }
    writeShort(override.default)
}

private fun ByteBuf.writeRecolour(recolour: Rs2OverrideRecolour) {
    writeShort(recolour.from)
    writeShort(recolour.to)
    writeShort(recolour.source)
    writeShort(recolour.destination)
}

private fun <T> ByteBuf.writeGroups(groups: List<Rs2OverrideGroup<T>>, entry: ByteBuf.(T) -> Unit) {
    writeByte(groups.size)
    for (group in groups) {
        writeByte(group.key)
        writeByte(group.entries.size)
        group.entries.forEach { entry(it) }
    }
}
