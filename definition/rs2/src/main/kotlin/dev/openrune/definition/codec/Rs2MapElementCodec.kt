package dev.openrune.definition.codec

import dev.openrune.definition.DefinitionCodec
import dev.openrune.definition.type.Rs2MapElementAlign
import dev.openrune.definition.type.Rs2MapElementCondition
import dev.openrune.definition.type.Rs2MapElementMulti
import dev.openrune.definition.type.Rs2MapElementPolygonPoint
import dev.openrune.definition.type.Rs2MapElementShow
import dev.openrune.definition.type.Rs2MapElementType
import dev.openrune.definition.util.readNullableLargeSmartCorrect
import dev.openrune.definition.util.readStringCP
import dev.openrune.definition.util.readUnsignedShortOrNull
import dev.openrune.definition.util.writeNullableLargeSmartCorrect
import dev.openrune.definition.util.writeStringCP
import io.netty.buffer.ByteBuf

/**
 * Ported from [zwyz/rs3-cache](https://github.com/zwyz/rs3-cache)'s `MapElementUnpacker.java`.
 *
 * [rev] is needed to decode opcode 15 (polygon): its outline layout changed at build 629 from a
 * single colour for the whole polygon to a per-point palette. Encoding always writes the modern
 * (>= 629) layout, and conditions/multi-variants always via their wide (opcode 250-253) form
 * rather than the narrow (9/20/26/27) one - both are strict supersets of the older layouts.
 */
class Rs2MapElementCodec(private val rev: Int) : DefinitionCodec<Rs2MapElementType> {

    override fun Rs2MapElementType.read(opcode: Int, buffer: ByteBuf) {
        when (opcode) {
            1 -> sprite = buffer.readNullableLargeSmartCorrect()
            2 -> mouseOverGraphic = buffer.readNullableLargeSmartCorrect()
            3 -> text = buffer.readStringCP()
            4 -> textColour = buffer.readUnsignedMedium()
            5 -> textMouseOverColour = buffer.readUnsignedMedium()
            6 -> textSize = buffer.readUnsignedByte().toInt()

            7 -> show = when (val value = buffer.readUnsignedByte().toInt()) {
                0 -> Rs2MapElementShow.NONE
                1 -> Rs2MapElementShow.MAP
                2 -> Rs2MapElementShow.MINIMAP
                3 -> Rs2MapElementShow.BOTH
                else -> error("Invalid show value $value")
            }

            8 -> mapFunction = buffer.readUnsignedByte().toInt() != 0

            9 -> condition = readConditionNarrow(buffer)
            250 -> condition = readConditionWide(buffer)

            10 -> op1 = buffer.readStringCP()
            11 -> op2 = buffer.readStringCP()
            12 -> op3 = buffer.readStringCP()
            13 -> op4 = buffer.readStringCP()
            14 -> op5 = buffer.readStringCP()

            15 -> readPolygon(buffer)

            16 -> listable = false
            17 -> opBase = buffer.readStringCP()
            18 -> worldMapArrow = buffer.readNullableLargeSmartCorrect()
            19 -> category = buffer.readUnsignedShort()

            20 -> condition2 = readConditionNarrow(buffer)
            251 -> condition2 = readConditionWide(buffer)

            21 -> textBackgroundOutline = buffer.readInt()
            22 -> textBackgroundFill = buffer.readInt()
            23 -> {
                polygonOutlineDashLength = buffer.readUnsignedByte().toInt()
                polygonOutlineDashGap = buffer.readUnsignedByte().toInt()
                polygonOutlineDashPhase = buffer.readUnsignedByte().toInt()
            }
            24 -> {
                textOffsetX = buffer.readShort().toInt()
                textOffsetY = buffer.readShort().toInt()
            }
            25 -> flashSprite = buffer.readNullableLargeSmartCorrect()

            26 -> multi = readMultiNarrow(buffer, hasDefault = false)
            252 -> multi = readMultiWide(buffer, hasDefault = false)
            27 -> multiDefault = readMultiNarrow(buffer, hasDefault = true)
            253 -> multiDefault = readMultiWide(buffer, hasDefault = true)

            28 -> minimapIconScale = buffer.readUnsignedByte().toInt()

            29 -> horizontalAlign = when (val value = buffer.readUnsignedByte().toInt()) {
                0 -> Rs2MapElementAlign.START
                1 -> Rs2MapElementAlign.CENTRE
                2 -> Rs2MapElementAlign.END
                else -> error("Invalid halign value $value")
            }

            30 -> verticalAlign = when (val value = buffer.readUnsignedByte().toInt()) {
                0 -> Rs2MapElementAlign.START
                1 -> Rs2MapElementAlign.CENTRE
                2 -> Rs2MapElementAlign.END
                else -> error("Invalid valign value $value")
            }

            249 -> readParameters(buffer)
        }
    }

    private fun Rs2MapElementType.readPolygon(buffer: ByteBuf) {
        val points = buffer.readUnsignedByte().toInt()
        val coords = List(points) { buffer.readShort().toInt() to buffer.readShort().toInt() }
        polygonFill = buffer.readInt()

        val outlines: List<Int> = if (rev < 629) {
            val colour = buffer.readInt()
            List(points) { colour }
        } else {
            val paletteSize = buffer.readUnsignedByte().toInt()
            val palette = IntArray(paletteSize) { buffer.readInt() }
            if (paletteSize == 1) {
                repeat(points) { buffer.readByte() }
                List(points) { palette[0] }
            } else {
                List(points) { palette[buffer.readByte().toInt()] }
            }
        }

        polygon = coords.mapIndexed { i, (x, y) -> Rs2MapElementPolygonPoint(x, y, outlines[i]) }
    }

    private fun readConditionNarrow(buffer: ByteBuf) =
        Rs2MapElementCondition(buffer.readUnsignedShortOrNull(), buffer.readUnsignedShortOrNull(), buffer.readInt(), buffer.readInt())

    private fun readConditionWide(buffer: ByteBuf) =
        Rs2MapElementCondition(buffer.readUnsignedMediumOrNull(), buffer.readUnsignedShortOrNull(), buffer.readInt(), buffer.readInt())

    private fun readMultiNarrow(buffer: ByteBuf, hasDefault: Boolean) = readMulti(buffer, hasDefault) { it.readUnsignedShortOrNull() }

    private fun readMultiWide(buffer: ByteBuf, hasDefault: Boolean) = readMulti(buffer, hasDefault) { it.readUnsignedMediumOrNull() }

    private inline fun readMulti(buffer: ByteBuf, hasDefault: Boolean, readVarbit: (ByteBuf) -> Int?): Rs2MapElementMulti {
        val varbit = readVarbit(buffer)
        val varp = buffer.readUnsignedShortOrNull()
        val default = if (hasDefault) buffer.readUnsignedShortOrNull() else null
        val count = buffer.readUnsignedByte().toInt()
        val values = List(count + 1) { buffer.readUnsignedShortOrNull() }
        return Rs2MapElementMulti(varbit, varp, default, values)
    }

    private fun ByteBuf.readUnsignedMediumOrNull(): Int? {
        val value = readUnsignedMedium()
        return if (value == 0xFFFFFF) null else value
    }

    override fun ByteBuf.encode(definition: Rs2MapElementType) {
        definition.sprite?.let {
            writeByte(1)
            writeNullableLargeSmartCorrect(it)
        }
        definition.mouseOverGraphic?.let {
            writeByte(2)
            writeNullableLargeSmartCorrect(it)
        }
        definition.text?.let {
            writeByte(3)
            writeStringCP(it)
        }
        if (definition.textColour != 0) {
            writeByte(4)
            writeMedium(definition.textColour)
        }
        if (definition.textMouseOverColour != 0) {
            writeByte(5)
            writeMedium(definition.textMouseOverColour)
        }
        if (definition.textSize != 0) {
            writeByte(6)
            writeByte(definition.textSize)
        }
        if (definition.show != Rs2MapElementShow.NONE) {
            writeByte(7)
            writeByte(definition.show.ordinal)
        }
        if (definition.mapFunction) {
            writeByte(8)
            writeByte(1)
        }
        definition.condition?.let {
            writeByte(250)
            writeConditionWide(it)
        }
        definition.op1?.let { writeByte(10); writeStringCP(it) }
        definition.op2?.let { writeByte(11); writeStringCP(it) }
        definition.op3?.let { writeByte(12); writeStringCP(it) }
        definition.op4?.let { writeByte(13); writeStringCP(it) }
        definition.op5?.let { writeByte(14); writeStringCP(it) }

        if (definition.polygon.isNotEmpty()) {
            writeByte(15)
            writePolygon(definition)
        }

        if (!definition.listable) writeByte(16)
        definition.opBase?.let { writeByte(17); writeStringCP(it) }
        definition.worldMapArrow?.let { writeByte(18); writeNullableLargeSmartCorrect(it) }
        if (definition.category != -1) {
            writeByte(19)
            writeShort(definition.category)
        }
        definition.condition2?.let {
            writeByte(251)
            writeConditionWide(it)
        }
        if (definition.textBackgroundOutline != 0) {
            writeByte(21)
            writeInt(definition.textBackgroundOutline)
        }
        if (definition.textBackgroundFill != 0) {
            writeByte(22)
            writeInt(definition.textBackgroundFill)
        }
        if (definition.polygonOutlineDashLength != 0 || definition.polygonOutlineDashGap != 0 || definition.polygonOutlineDashPhase != 0) {
            writeByte(23)
            writeByte(definition.polygonOutlineDashLength)
            writeByte(definition.polygonOutlineDashGap)
            writeByte(definition.polygonOutlineDashPhase)
        }
        if (definition.textOffsetX != 0 || definition.textOffsetY != 0) {
            writeByte(24)
            writeShort(definition.textOffsetX)
            writeShort(definition.textOffsetY)
        }
        definition.flashSprite?.let { writeByte(25); writeNullableLargeSmartCorrect(it) }

        definition.multi?.let { writeByte(252); writeMultiWide(it) }
        definition.multiDefault?.let { writeByte(253); writeMultiWide(it, hasDefault = true) }

        if (definition.minimapIconScale != 0) {
            writeByte(28)
            writeByte(definition.minimapIconScale)
        }
        if (definition.horizontalAlign != Rs2MapElementAlign.START) {
            writeByte(29)
            writeByte(definition.horizontalAlign.ordinal)
        }
        if (definition.verticalAlign != Rs2MapElementAlign.START) {
            writeByte(30)
            writeByte(definition.verticalAlign.ordinal)
        }

        definition.writeParameters(this)

        writeByte(0)
    }

    private fun ByteBuf.writePolygon(definition: Rs2MapElementType) {
        val points = definition.polygon
        writeByte(points.size)
        points.forEach {
            writeShort(it.x)
            writeShort(it.y)
        }
        writeInt(definition.polygonFill)

        val palette = points.map { it.outlineColour }.distinct()
        writeByte(palette.size)
        palette.forEach { writeInt(it) }

        if (palette.size == 1) {
            repeat(points.size) { writeByte(0) }
        } else {
            points.forEach { writeByte(palette.indexOf(it.outlineColour)) }
        }
    }

    private fun ByteBuf.writeConditionWide(condition: Rs2MapElementCondition) {
        writeMedium(condition.varbit ?: 0xFFFFFF)
        writeShort(condition.varp ?: 0xFFFF)
        writeInt(condition.op)
        writeInt(condition.value)
    }

    private fun ByteBuf.writeMultiWide(multi: Rs2MapElementMulti, hasDefault: Boolean = false) {
        writeMedium(multi.varbit ?: 0xFFFFFF)
        writeShort(multi.varp ?: 0xFFFF)
        if (hasDefault) writeShort(multi.default ?: 0xFFFF)
        writeByte(multi.values.size - 1)
        multi.values.forEach { writeShort(it ?: 0xFFFF) }
    }

    override fun createDefinition() = Rs2MapElementType(0)
}
