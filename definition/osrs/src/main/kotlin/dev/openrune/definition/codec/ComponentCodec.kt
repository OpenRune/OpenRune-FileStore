package dev.openrune.definition.codec

import dev.openrune.definition.DefinitionCodec
import dev.openrune.definition.type.widget.ComponentType
import dev.openrune.definition.util.readString
import dev.openrune.definition.util.writeString
import io.netty.buffer.ByteBuf
import java.util.*

class ComponentCodec : DefinitionCodec<ComponentType> {

    override fun ComponentType.read(opcode: Int, buffer: ByteBuf) {
        if (buffer.array().isNotEmpty()) {
            isIf3 = buffer.getByte(0).toInt() == -1
            if (isIf3) {
                decodeIf3(buffer)
            } else {
                decode(buffer)
            }
        }

    }

    private fun ComponentType.decode(buffer: ByteBuf) {
        type = buffer.readUnsignedByte().toInt()
        menuType = buffer.readUnsignedByte().toInt()
        contentType = buffer.readUnsignedShort()
        x = buffer.readShort().toInt()
        y = buffer.readShort().toInt()
        width = buffer.readUnsignedShort()
        height = buffer.readUnsignedShort()
        opacity = buffer.readUnsignedByte().toInt()
        parentId = buffer.readUnsignedShort().let { if (it == 65535) -1 else it + (-1 and -65536) }
        hoveredSiblingId = buffer.readUnsignedShort().let { if (it == 65535) -1 else it }

        val var2 = buffer.readUnsignedByte().toInt()
        if (var2 > 0) {
            valueCompareType = IntArray(var2) { buffer.readUnsignedByte().toInt() }
            requiredValues = IntArray(var2) { buffer.readUnsignedShort() }
        }

        val var3 = buffer.readUnsignedByte().toInt()
        if (var3 > 0) {
            dynamicValues = Array(var3) {
                IntArray(buffer.readUnsignedShort()) {
                    buffer.readUnsignedShort().let { if (it == 65535) -1 else it }
                }
            }
        }

        when (type) {
            0 -> {
                scrollHeight = buffer.readUnsignedShort()
                hidden = buffer.readUnsignedByte().toInt() == 1
            }
            1 -> {
                buffer.readShort()
                buffer.readByte()
            }
            2 -> {
                itemIds = IntArray(width * height)
                itemQuantities = IntArray(width * height)
                if (buffer.readUnsignedByte().toInt() == 1) accessMask = accessMask or 268435456
                if (buffer.readUnsignedByte().toInt() == 1) accessMask = accessMask or 1073741824
                if (buffer.readUnsignedByte().toInt() == 1) accessMask = accessMask or Int.MIN_VALUE
                if (buffer.readUnsignedByte().toInt() == 1) accessMask = accessMask or 536870912
                xPitch = buffer.readUnsignedByte().toInt()
                yPitch = buffer.readUnsignedByte().toInt()
                xOffsets = IntArray(20)
                yOffsets = IntArray(20)
                sprites = IntArray(20) { if (buffer.readUnsignedByte().toInt() == 1) buffer.readInt() else -1 }
                configActions = Array(5) { buffer.readString().takeIf { it.isNotEmpty() }!! }
            }
            3 -> filled = buffer.readUnsignedByte().toInt() == 1
            4, 1 -> {
                xAllignment = buffer.readUnsignedByte().toInt()
                yAllignment = buffer.readUnsignedByte().toInt()
                lineHeight = buffer.readUnsignedByte().toInt()
                font = buffer.readUnsignedShort().let { if (it == 65535) -1 else it }
                textShadowed = buffer.readUnsignedByte().toInt() == 1
            }
            4 -> {
                text = buffer.readString()
                alternateText = buffer.readString()
            }
            1, 3, 4 -> {
                color = buffer.readInt()
                alternateTextColor = buffer.readInt()
                hoveredTextColor = buffer.readInt()
                alternateHoveredTextColor = buffer.readInt()
            }
            5 -> {
                spriteId = buffer.readInt()
                alternateSpriteId = buffer.readInt()
            }
            6 -> {
                modelType = 1
                modelId = buffer.readUnsignedShort().let { if (it == 65535) -1 else it }
                field2840 = 1
                alternateModelId = buffer.readUnsignedShort().let { if (it == 65535) -1 else it }
                animation = buffer.readUnsignedShort().let { if (it == 65535) -1 else it }
                alternateAnimation = buffer.readUnsignedShort().let { if (it == 65535) -1 else it }
                modelZoom = buffer.readUnsignedShort()
                rotationX = buffer.readUnsignedShort()
                rotationY = buffer.readUnsignedShort()
            }
            7 -> {
                itemIds = IntArray(height * width)
                itemQuantities = IntArray(width * height)
                xAllignment = buffer.readUnsignedByte().toInt()
                font = buffer.readUnsignedShort().let { if (it == 65535) -1 else it }
                textShadowed = buffer.readUnsignedByte().toInt() == 1
                color = buffer.readInt()
                xPitch = buffer.readShort().toInt()
                yPitch = buffer.readShort().toInt()
                if (buffer.readUnsignedByte().toInt() == 1) accessMask = accessMask or 1073741824
                configActions = Array(5) { buffer.readString().takeIf { it.isNotEmpty() }!! }
            }
            8 -> text = buffer.readString()
        }

        if (menuType == 2 || type == 2) {
            targetVerb = buffer.readString()
            spellName = buffer.readString()
            accessMask = accessMask or (buffer.readUnsignedShort() and 63 shl 11)
        }

        if (menuType in listOf(1, 4, 5, 6)) {
            tooltip = buffer.readString().ifEmpty {
                when (menuType) {
                    1 -> "Ok"
                    4, 5 -> "Select"
                    6 -> "Continue"
                    else -> tooltip
                }
            }
        }

        if (menuType in listOf(1, 4, 5)) {
            accessMask = accessMask or 4194304
        }
        if (menuType == 6) {
            accessMask = accessMask or 1
        }
    }


    fun ComponentType.decodeIf3(buffer: ByteBuf) {
        buffer.readByte()
        type = buffer.readUnsignedByte().toInt()

        contentType = buffer.readUnsignedShort()
        x = buffer.readShort().toInt()
        y = buffer.readShort().toInt()
        width = buffer.readUnsignedShort()
        height = if (type == 9) buffer.readShort().toInt() else buffer.readUnsignedShort()
        widthMode = buffer.readByte().toInt()
        heightMode = buffer.readByte().toInt()
        xMode = buffer.readByte().toInt()
        yMode = buffer.readByte().toInt()
        parentId = buffer.readUnsignedShort().let { if (it == 65535) -1 else it + (-1 and -65536) }
        hidden = buffer.readUnsignedByte().toInt() == 1

        if (type == 0) {
            scrollWidth = buffer.readUnsignedShort()
            scrollHeight = buffer.readUnsignedShort()
            noClickThrough = buffer.readUnsignedByte().toInt() == 1
        }
        if (type == 5) {
            spriteId = buffer.readInt()
            textureId = buffer.readUnsignedShort()
            spriteTiling = buffer.readUnsignedByte().toInt() == 1
            opacity = buffer.readUnsignedByte().toInt()
            borderType = buffer.readUnsignedByte().toInt()
            shadowColor = buffer.readInt()
            flippedVertically = buffer.readUnsignedByte().toInt() == 1
            flippedHorizontally = buffer.readUnsignedByte().toInt() == 1
        }
        if (type == 6) {
            modelType = 1
            modelId = buffer.readUnsignedShort().let { if (it == 65535) -1 else it }
            offsetX2d = buffer.readShort().toInt()
            offsetY2d = buffer.readShort().toInt()
            rotationX = buffer.readUnsignedShort()
            rotationY = buffer.readUnsignedShort()
            rotationZ = buffer.readUnsignedShort()
            modelZoom = buffer.readUnsignedShort()
            animation = buffer.readUnsignedShort().let { if (it == 65535) -1 else it }
            orthogonal = buffer.readUnsignedByte().toInt() == 1
            buffer.readShort()
            if (widthMode != 0) modelHeightOverride = buffer.readUnsignedShort()
            if (heightMode != 0) buffer.readShort()
        }
        if (type == 4) {
            font = buffer.readUnsignedShort().let { if (it == 65535) -1 else it }
            text = buffer.readString()
            lineHeight = buffer.readUnsignedByte().toInt()
            xAllignment = buffer.readUnsignedByte().toInt()
            yAllignment = buffer.readUnsignedByte().toInt()
            textShadowed = buffer.readUnsignedByte().toInt() == 1
            color = buffer.readInt()
        }
        if (type == 3) {
            color = buffer.readInt()
            filled = buffer.readUnsignedByte().toInt() == 1
            opacity = buffer.readUnsignedByte().toInt()
        }
        if (type == 9) {
            lineWidth = buffer.readUnsignedByte().toInt()
            color = buffer.readInt()
            lineDirection = buffer.readUnsignedByte().toInt() == 1
        }

        accessMask = buffer.readMedium()
        opBase = buffer.readString()
        val var2 = buffer.readUnsignedByte().toInt()
        if (var2 > 0) {
            actions = Array(var2) { buffer.readString() }
        }
        dragDeadZone = buffer.readUnsignedByte().toInt()
        dragDeadTime = buffer.readUnsignedByte().toInt()
        dragRenderBehavior = buffer.readUnsignedByte().toInt() == 1
        targetVerb = buffer.readString()

        decodeListener(buffer)
        onMouseOverListener = decodeListener(buffer)
        onMouseLeaveListener = decodeListener(buffer)
        onTargetLeaveListener = decodeListener(buffer)
        onTargetEnterListener = decodeListener(buffer)
        onVarTransmitListener = decodeListener(buffer)
        onInvTransmitListener = decodeListener(buffer)
        onStatTransmitListener = decodeListener(buffer)
        onTimerListener = decodeListener(buffer)
        onOpListener = decodeListener(buffer)
        onMouseRepeatListener = decodeListener(buffer)
        onClickListener = decodeListener(buffer)
        onClickRepeatListener = decodeListener(buffer)
        onReleaseListener = decodeListener(buffer)
        onHoldListener = decodeListener(buffer)
        onDragListener = decodeListener(buffer)
        onDragCompleteListener = decodeListener(buffer)
        onScrollWheelListener = decodeListener(buffer)

        varTransmitTriggers = decodeTransmitList(buffer)
        invTransmitTriggers = decodeTransmitList(buffer)
        statTransmitTriggers = decodeTransmitList(buffer)
    }

    private fun decodeListener(buffer: ByteBuf): Array<Any>? {
        val int0 = buffer.readUnsignedByte().toInt()
        return if (int0 == 0) {
            null
        } else {
            Array(int0) {
                when (val int2 = buffer.readUnsignedByte().toInt()) {
                    0 -> buffer.readInt()
                    1 -> buffer.readString()
                    else -> throw IllegalArgumentException("Unknown type: $int2")
                }
            }
        }
    }

    private fun decodeTransmitList(buffer: ByteBuf): IntArray? {
        val int0 = buffer.readUnsignedByte().toInt()
        return if (int0 == 0) {
            null
        } else {
            IntArray(int0) { buffer.readInt() }
        }
    }


    override fun ByteBuf.encode(definition: ComponentType) {
        if (definition.isIf3) {
            writeByte(-1)
            writeByte(definition.type)
            writeShort(definition.contentType)
            writeShort(definition.x)
            writeShort(definition.y)
            writeShort(definition.width)
            writeShort(definition.height)
            writeByte(definition.widthMode)
            writeByte(definition.heightMode)
            writeByte(definition.xMode)
            writeByte(definition.yMode)
            writeShort(if (definition.parentId == -1) 65535 else definition.parentId)
            writeByte(if (definition.hidden) 1 else 0)
            if (definition.type == 0) {
                writeShort(definition.scrollWidth)
                writeShort(definition.scrollHeight)
                writeByte(if (definition.noClickThrough) 1 else 0)
            }
            if (definition.type == 5) {
                writeInt(definition.spriteId)
                writeShort(definition.textureId)
                writeByte(if (definition.spriteTiling) 1 else 0)
                writeByte(definition.opacity)
                writeByte(definition.borderType)
                writeInt(definition.shadowColor)
                writeByte(if (definition.flippedVertically) 1 else 0)
                writeByte(if (definition.flippedHorizontally) 1 else 0)
            }
            if (definition.type == 6) {
                writeShort(if (definition.modelId == -1) 65535 else definition.modelId)
                writeShort(definition.offsetX2d)
                writeShort(definition.offsetY2d)
                writeShort(definition.rotationX)
                writeShort(definition.rotationY)
                writeShort(definition.rotationZ)
                writeShort(definition.modelZoom)
                writeShort(if (definition.animation == -1) 65535 else definition.animation)
                writeByte(if (definition.orthogonal) 1 else 0)
                writeShort(5)
                if (definition.widthMode != 0) {
                    writeShort(definition.modelHeightOverride)
                }
                if (definition.heightMode != 0) {
                    writeShort(5)
                }
            }
            if (definition.type == 4) {
                writeShort(if (definition.font == -1) 65535 else definition.font)
                writeString(definition.text)
                writeByte(definition.lineHeight)
                writeByte(definition.xAllignment)
                writeByte(definition.yAllignment)
                writeByte((if (definition.textShadowed) 1 else 0))
                writeInt(definition.color)
            }
            if (definition.type == 3) {
                writeInt(definition.color)
                writeByte(if (definition.filled) 1 else 0)
                writeByte(definition.opacity)
            }
            if (definition.type == 9) {
                writeByte(definition.lineWidth)
                writeInt(definition.color)
                writeByte(if (definition.lineDirection) 1 else 0)
            }
            writeMedium(definition.accessMask)
            writeString(definition.opBase)
            val len = if (definition.actions == null) 0 else definition.actions!!.size
            writeByte(len)
            for (i in 0 until len) {
                writeString(Optional.ofNullable<String>(definition.actions!!.get(i)).orElse(""))
            }
            writeByte(definition.dragDeadZone)
            writeByte(definition.dragDeadTime)
            writeByte((if (definition.dragRenderBehavior) 1 else 0))
            writeString(definition.targetVerb)
            definition.encodeListener(this, definition.onLoadListener)
            definition.encodeListener(this, definition.onMouseOverListener)
            definition.encodeListener(this, definition.onMouseLeaveListener)
            definition.encodeListener(this, definition.onTargetLeaveListener)
            definition.encodeListener(this, definition.onTargetEnterListener)
            definition.encodeListener(this, definition.onVarTransmitListener)
            definition.encodeListener(this, definition.onInvTransmitListener)
            definition.encodeListener(this, definition.onStatTransmitListener)
            definition.encodeListener(this, definition.onTimerListener)
            definition.encodeListener(this, definition.onOpListener)
            definition.encodeListener(this, definition.onMouseRepeatListener)
            definition.encodeListener(this, definition.onClickListener)
            definition.encodeListener(this, definition.onClickRepeatListener)
            definition.encodeListener(this, definition.onReleaseListener)
            definition.encodeListener(this, definition.onHoldListener)
            definition.encodeListener(this, definition.onDragListener)
            definition.encodeListener(this, definition.onDragCompleteListener)
            definition.encodeListener(this, definition.onScrollWheelListener)
            encodeTransmitList(this, definition.varTransmitTriggers)
            encodeTransmitList(this, definition.invTransmitTriggers)
            encodeTransmitList(this, definition.statTransmitTriggers)
        } else {
            //if1
            writeByte(definition.type)
            writeByte(definition.menuType)
            writeShort(definition.contentType)
            writeShort(definition.x)
            writeShort(definition.y)
            writeShort(definition.width)
            writeShort(definition.height)
            writeByte(definition.opacity)
            writeShort((if (definition.parentId == -1) 65535 else definition.parentId))
            writeShort((if (definition.hoveredSiblingId == -1) 65535 else definition.hoveredSiblingId))
            val len = if (definition.valueCompareType == null) 0 else definition.valueCompareType!!.size
            writeByte(len)
            for (i in 0 until len) {
                writeByte(definition.valueCompareType!!.get(i))
                writeShort(definition.requiredValues!!.get(i))
            }
            writeByte(if (definition.dynamicValues == null) 0 else definition.dynamicValues!!.size)
            if (definition.dynamicValues != null) {
                for (i in definition.dynamicValues!!.indices) {
                    writeShort(definition.dynamicValues!![i].size)
                    for (i2 in definition.dynamicValues!![i].indices) {
                        writeShort(definition.dynamicValues!![i][i2])
                    }
                }
            }
            if (definition.type == 0) {
                writeShort(definition.scrollHeight)
                writeByte((if (definition.hidden) 1 else 0))
            }
            if (definition.type == 1) {
                writeShort(0)
                writeByte(0)
            }
            if (definition.type == 2) {
                writeByte((if (((definition.accessMask and (1 shl 28)) != 1)) 1 else 0))
                writeByte((if (((definition.accessMask and (1 shl 30)) != 1)) 1 else 0))
                writeByte((if (((definition.accessMask and (1 shl 31)) != 1)) 1 else 0))
                writeByte((if (((definition.accessMask and (1 shl 29)) != 1)) 1 else 0))
                writeByte(definition.xPitch)
                writeByte(definition.yPitch)
                for (i in 0..19) {
                    if (definition.sprites!!.get(i) == -1) {
                        writeByte(0)
                    } else {
                        writeByte(1)
                        writeShort(definition.xOffsets!!.get(i))
                        writeShort(definition.yOffsets!!.get(i))
                        writeInt(definition.sprites!!.get(i))
                    }
                }
                for (i in 0..4) {
                    writeString(definition.configActions!![i])
                }
            }
            if (definition.type == 3) {
                writeByte((if (definition.filled) 1 else 0))
            }
            if (definition.type == 4 || definition.type == 1) {
                writeByte(definition.xAllignment)
                writeByte(definition.yAllignment)
                writeByte(definition.lineHeight)
                writeShort(definition.font)
                writeByte(if (definition.textShadowed) 1 else 0)
            }
            if (definition.type == 4) {
                writeString(definition.text)
                writeString(definition.alternateText)
            }
            if (definition.type == 1 || definition.type == 3 || definition.type == 4) {
                writeInt(definition.color)
            }
            if (definition.type == 3 || definition.type == 4) {
                writeInt(definition.alternateTextColor)
                writeInt(definition.hoveredTextColor)
                writeInt(definition.alternateHoveredTextColor)
            }
            if (definition.type == 5) {
                writeInt(definition.spriteId)
                writeInt(definition.alternateSpriteId)
            }
            if (definition.type == 6) {
                writeShort(definition.modelId)
                writeShort(definition.alternateModelId)
                writeShort(definition.animation)
                writeShort(definition.alternateAnimation)
                writeShort(definition.modelZoom)
                writeShort(definition.rotationX)
                writeShort(definition.rotationY)
            }
            if (definition.type == 7) {
                writeByte(definition.xAllignment)
                writeShort(definition.font)
                writeByte((if (definition.textShadowed) 1 else 0))
                writeInt(definition.color)
                writeShort(definition.xPitch)
                writeShort(definition.yPitch)
                writeByte((if (((definition.accessMask and (1 shl 30)) != 1)) 1 else 0))
                for (i in 0..4) {
                    writeString(definition.configActions!![i])
                }
            }
            if (definition.type == 8) {
                writeString(definition.text)
            }
            if (definition.menuType == 2 || definition.type == 2) {
                writeString(definition.targetVerb)
                writeString(definition.spellName)
                writeShort(((definition.accessMask shr 11) and 63))
            }
            if (definition.menuType == 1 || definition.menuType == 4 || definition.menuType == 5 || definition.menuType == 6) {
                writeString(definition.tooltip)
            }
        }
    }

    private fun ComponentType.encodeListener(buffer: ByteBuf, objectArray: Array<Any>?) {
        buffer.writeByte(objectArray?.size ?: 0)
        if (objectArray == null) {
            return
        }
        for (i in objectArray.indices) {
            var `object` = objectArray[i]
            if (`object` is String && `object`.contains("component")) {
                `object` = pack(id, child.toString())
            }
            if (`object` is Int) {
                buffer.writeByte(0)
                buffer.writeInt(`object`)
            } else {
                buffer.writeByte(1)
                buffer.writeString(`object` as String?)
            }
        }
    }

    fun encodeTransmitList(buffer: ByteBuf, intArray: IntArray?) {
        buffer.writeByte(intArray?.size ?: 0)
        if (intArray == null) {
            return
        }
        for (i in intArray.indices) {
            buffer.writeInt(intArray[i])
        }
    }

    private fun pack(interfaceId: Int, componentId: String): Int {
        return (interfaceId and 0xFFFF) shl 16 or (componentId.toInt() and 0xFFFF)
    }

    override fun createDefinition() = ComponentType()

    override fun readLoop(definition: ComponentType, buffer: ByteBuf) {
        definition.read(-1, buffer)
    }

}