package dev.openrune.cache.filestore.definition

import dev.openrune.cache.INTERFACES
import dev.openrune.cache.gameval.GameValHandler
import dev.openrune.cache.gameval.GameValHandler.lookup
import dev.openrune.cache.gameval.GameValHandler.lookupAs
import dev.openrune.cache.gameval.impl.Interface
import dev.openrune.definition.GameValGroupTypes
import dev.openrune.definition.type.widget.ComponentType
import dev.openrune.definition.type.widget.ComponentTypeBuilder
import dev.openrune.definition.util.TextUtil
import dev.openrune.definition.util.readString
import dev.openrune.definition.util.readUnsignedShortOrNull
import dev.openrune.definition.util.writeString
import dev.openrune.filesystem.Cache
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import kotlin.collections.indices
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

/**
 * Internal custom type used to represent a game interface and all of its linked components.
 *
 * This class is primarily used for tracking interface definitions, computing a stable identity
 * hash, and comparing interface structures based on their internal ID and component map.
 *
 * @property components The map of component IDs to their associated [ComponentType].
 * @property _internalId Optional internal numeric identifier for this interface.
 * @property _internalName Optional internal name for this interface.
 */
public data class InterfaceType(
    public val components: Map<Int,ComponentType>,
    internal var _internalId: Int?,
    internal var _internalName: String?,
) {
    private val identityHash by lazy { computeIdentityHash() }

    val id: Int
        get() = _internalId ?: error("`internalId` must not be null.")

    val internalName: String
        get() = _internalName ?: error("`internalName` must not be null.")

    public fun computeIdentityHash(): Long {
        var result = id.hashCode().toLong()
        result = 61 * result + components.hashCode()
        return result and 0x7FFFFFFFFFFFFFFF
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is InterfaceType) return false
        if (other.id != id) return false
        if (other.components != components) return false
        return true
    }

    override fun hashCode(): Int = computeIdentityHash().toInt()

    override fun toString(): String {
        val sb = StringBuilder()
        sb.append("InterfaceType(")
        sb.append("id=$id, ")
        sb.append("internalName='${_internalName ?: "null"}', ")
        sb.append("componentCount=${components.size}")
        sb.append(")\n")
        
        if (components.isNotEmpty()) {
            sb.append("Components:\n")
            components.entries.sortedBy { it.key }.forEach { (index, component) ->
                sb.append("  [$index] ${component.toString()}\n")
            }
        } else {
            sb.append("Components: (none)\n")
        }
        
        return sb.toString()
    }

      companion object{

          @OptIn(ExperimentalContracts::class)
          public fun InterfaceType?.isType(other:InterfaceType): Boolean {
              contract { returns(true) implies (this@isType != null) }
              return this != null && this.id == other.id
          }

      }

}

class ComponentDecoder(private val cache: Cache) {

    val gamevals = GameValHandler.readGameVal(GameValGroupTypes.IFTYPES,cache)

    fun load(components : MutableMap<Int,InterfaceType>) {
        val groups = cache.archives(INTERFACES)
        for (group in groups) {
            val files = cache.files(INTERFACES, group)
            val types = mutableMapOf<Int, ComponentType>()
            for (file in files) {
                val combinedId = (group shl 16) or file
                val data = cache.data(INTERFACES, group, file)
                val interfaceId = (combinedId ushr 16) and 0xFFFF
                val childIdRaw = combinedId and 0xFFFF

                val componentName = gamevals.lookupAs<Interface>(interfaceId)?.components?.lookup(childIdRaw)?.name
                if (componentName != null) {
                    types[file] = read(combinedId, Unpooled.wrappedBuffer(data),componentName)
                }
            }

            components[group] = InterfaceType(
                types.toMap(),
                group,
                gamevals.lookup(group)?.name
            )
        }
    }

    fun read(combinedId : Int,buffer: ByteBuf,componentName : String) = decode(combinedId, buffer,componentName).build(combinedId)

    private fun decode(combinedId: Int, data: ByteBuf, componentName : String): ComponentTypeBuilder {
        val builder = ComponentTypeBuilder(componentName)

        val version = data.getByte(data.readerIndex()).toInt()
        if (version == -1) {
            decodeV3(combinedId, builder, data)
        } else {
            decodeV1(combinedId, builder, data)
        }
        return builder
    }

    public fun decodeV3(combinedId: Int, builder: ComponentTypeBuilder, data: ByteBuf): Unit =
        with(builder) {
            data.readByte()
            v3 = true
            type = data.readUnsignedByte().toInt()
            contentType = data.readUnsignedShort()
            x = data.readShort().toInt()
            y = data.readShort().toInt()
            width = data.readUnsignedShort()
            height =
                if (type == 9) {
                    data.readShort().toInt()
                } else {
                    data.readUnsignedShort()
                }

            widthMode = data.readByte().toInt()
            heightMode = data.readByte().toInt()
            xMode = data.readByte().toInt()
            yMode = data.readByte().toInt()
            var layer = data.readUnsignedShortOrNull()
            if (layer != null) {
                layer += combinedId and -65536
            }
            this.layer = layer

            hide = data.readBoolean()

            if (type == 0) {
                scrollWidth = data.readUnsignedShort()
                scrollHeight = data.readUnsignedShort()
                noClickThrough = data.readBoolean()
            }

            if (type == 5) {
                graphic = data.readInt()
                angle2d = data.readUnsignedShort()
                tiling = data.readBoolean()
                trans1 = data.readUnsignedByte().toInt()
                outline = data.readUnsignedByte().toInt()
                graphicShadow = data.readInt()
                vFlip = data.readBoolean()
                hFlip = data.readBoolean()
            }

            if (type == 6) {
                modelKind = 1
                model = data.readUnsignedShortOrNull()

                modelX = data.readShort().toInt()
                modelY = data.readShort().toInt()
                modelAngleX = data.readUnsignedShort()
                modelAngleY = data.readUnsignedShort()
                modelAngleZ = data.readUnsignedShort()
                modelZoom = data.readUnsignedShort()
                modelAnim = data.readUnsignedShortOrNull()

                modelOrthog = data.readBoolean()
                data.readUnsignedShort()

                if (widthMode != 0) {
                    modelObjWidth = data.readUnsignedShort()
                }

                if (heightMode != 0) {
                    data.readUnsignedShort()
                }
            }

            if (type == 4) {
                textFont = data.readUnsignedShortOrNull()
                text = data.readString()
                textLineHeight = data.readUnsignedByte().toInt()
                textAlignH = data.readUnsignedByte().toInt()
                textAlignV = data.readUnsignedByte().toInt()
                textShadow = data.readBoolean()
                colour1 = data.readInt()
            }

            if (type == 3) {
                colour1 = data.readInt()
                fill = data.readBoolean()
                trans1 = data.readUnsignedByte().toInt()
            }

            if (type == 9) {
                lineWid = data.readUnsignedByte().toInt()
                colour1 = data.readInt()
                lineDirection = data.readBoolean()
            }

            events = data.readUnsignedMedium()
            opBase = data.readString()

            val opCount = data.readUnsignedByte().toInt()
            if (opCount > 0) {
                val op = Array(opCount) { "" }
                for (i in 0 until opCount) {
                    op[i] = data.readString()
                }
                this.op = op
            }

            dragDeadZone = data.readUnsignedByte().toInt()
            dragDeadTime = data.readUnsignedByte().toInt()
            draggableBehavior = data.readBoolean()
            targetVerb = data.readString()
            onLoad = decodeHook(data)
            onMouseOver = decodeHook(data)
            onMouseLeave = decodeHook(data)
            onTargetLeave = decodeHook(data)
            onTargetEnter = decodeHook(data)
            onVarTransmit = decodeHook(data)
            onInvTransmit = decodeHook(data)
            onStatTransmit = decodeHook(data)
            onTimer = decodeHook(data)
            onOp = decodeHook(data)
            onMouseRepeat = decodeHook(data)
            onClick = decodeHook(data)
            onClickRepeat = decodeHook(data)
            onRelease = decodeHook(data)
            onHold = decodeHook(data)
            onDrag = decodeHook(data)
            onDragComplete = decodeHook(data)
            onScrollWheel = decodeHook(data)
            onVarTransmitList = decodeHookTransmitList(data)
            onInvTransmitList = decodeHookTransmitList(data)
            onStatTransmitList = decodeHookTransmitList(data)
        }

    public fun decodeV1(combinedId: Int, builder: ComponentTypeBuilder, data: ByteBuf): Unit =
        with(builder) {
            v3 = false
            type = data.readUnsignedByte().toInt()
            buttonType = data.readUnsignedByte().toInt()
            contentType = data.readUnsignedShort()
            x = data.readShort().toInt()
            y = data.readShort().toInt()
            width = data.readUnsignedShort()
            height = data.readUnsignedShort()
            trans1 = data.readUnsignedByte().toInt()
            var layer = data.readUnsignedShortOrNull()
            if (layer != null) {
                layer += combinedId and -65536
            }
            this.layer = layer

            mouseOverRedirect = data.readUnsignedShortOrNull()

            val cs1ComparisonCount = data.readUnsignedByte().toInt()
            if (cs1ComparisonCount > 0) {
                val comparisons = ShortArray(cs1ComparisonCount)
                val values = IntArray(cs1ComparisonCount)
                for (i in 0 until cs1ComparisonCount) {
                    comparisons[i] = data.readUnsignedByte()
                    values[i] = data.readUnsignedShort()
                }
                this.cs1Comparisons = comparisons
                this.cs1ComparisonValues = values
            }

            val cs1InstructionCount = data.readUnsignedByte().toInt()
            if (cs1InstructionCount > 0) {
                val instructions = Array(cs1InstructionCount) { intArrayOf() }
                for (i in 0 until cs1InstructionCount) {
                    val innerCount = data.readUnsignedShort()
                    val innerInstructions = IntArray(innerCount)
                    for (j in 0 until innerCount) {
                        innerInstructions[j] = data.readUnsignedShortOrNull() ?: -1
                    }
                    instructions[i] = innerInstructions
                }
                cs1Instructions = instructions
            }

            if (type == 0) {
                scrollHeight = data.readUnsignedShort()
                hide = data.readBoolean()
            }

            if (type == 1) {
                data.readUnsignedShort()
                data.readUnsignedByte()
            }

            if (type == 3) {
                fill = data.readBoolean()
            }

            if (type == 4 || type == 1) {
                textAlignH = data.readUnsignedByte().toInt()
                textAlignV = data.readUnsignedByte().toInt()
                textLineHeight = data.readUnsignedByte().toInt()
                textFont = data.readUnsignedShortOrNull()
                textShadow = data.readBoolean()
            }

            if (type == 4) {
                text = data.readString()
                secondaryText = data.readString()
            }

            if (type == 1 || type == 3 || type == 4) {
                colour1 = data.readInt()
            }

            if (type == 3 || type == 4) {
                colour2 = data.readInt()
                mouseOverColour1 = data.readInt()
                mouseOverColour2 = data.readInt()
            }

            if (type == 5) {
                graphic = data.readInt()
                secondaryGraphic = data.readInt()
            }

            if (type == 6) {
                modelKind = 1
                model = data.readUnsignedShortOrNull()

                secondaryModelKind = 1
                secondaryModel = data.readUnsignedShortOrNull()

                modelAnim = data.readUnsignedShortOrNull()
                secondaryModelAnim = data.readUnsignedShortOrNull()

                modelZoom = data.readUnsignedShort()
                modelAngleX = data.readUnsignedShort()
                modelAngleY = data.readUnsignedShort()
            }

            if (type == 8) {
                text = data.readString()
            }

            if (buttonType == 2) {
                targetVerb = data.readString()
                targetBase = data.readString()
                val events = data.readUnsignedShort() and 63
                this.events = events or (events shl 11)
            }

            if (buttonType == 1 || buttonType == 4 || buttonType == 5 || buttonType == 6) {
                buttonText = data.readString()
                if (buttonText.isNullOrEmpty()) {
                    if (buttonType == 1) {
                        buttonText = TextUtil.OK
                    }

                    if (buttonType == 4) {
                        buttonText = TextUtil.SELECT
                    }

                    if (buttonType == 5) {
                        buttonText = TextUtil.SELECT
                    }

                    if (buttonType == 6) {
                        buttonText = TextUtil.CONTINUE
                    }
                }
            }

            if (buttonType == 1 || buttonType == 4 || buttonType == 5) {
                val events = events ?: 0
                this.events = events or 4194304
            }

            if (buttonType == 6) {
                val events = events ?: 0
                this.events = events or 1
            }
        }

    public fun decodeHook(data: ByteBuf): Array<Any>? {
        val count = data.readUnsignedByte().toInt()
        if (count == 0) {
            return null
        }
        val values = Array<Any>(count) {}
        for (i in 0 until count) {
            val type = data.readUnsignedByte().toInt()
            values[i] =
                if (type == 0) {
                    Integer.valueOf(data.readInt())
                } else {
                    data.readString()
                }
        }
        return values
    }

    public fun decodeHookTransmitList(data: ByteBuf): IntArray? {
        val count = data.readUnsignedByte().toInt()
        if (count == 0) {
            return null
        }
        val values = IntArray(count) { data.readInt() }
        return values
    }


    public fun encode(type: ComponentType, data: ByteBuf) {
        if (type.v3) {
            encodeV3(type, data)
        } else {
            encodeV1(type, data)
        }
    }

    public fun encodeV1(unpacked: ComponentType, data: ByteBuf): Unit =
        with(unpacked) {
            data.writeByte(type)
            data.writeByte(buttonType)
            data.writeShort(clientCode)
            data.writeShort(x)
            data.writeShort(y)
            data.writeShort(width)
            data.writeShort(height)
            data.writeByte(trans1)
            data.writeShort(layer)
            data.writeShort(mouseOverRedirect)

            data.writeByte(cs1Comparisons?.size ?: 0)
            val cs1Comparisons = cs1Comparisons
            val cs1ComparisonValues = cs1ComparisonValues
            if (cs1Comparisons != null && cs1ComparisonValues != null) {
                for (i in cs1Comparisons.indices) {
                    data.writeByte(cs1Comparisons[i].toInt())
                    data.writeShort(cs1ComparisonValues[i].toInt())
                }
            }

            data.writeByte(cs1Instructions?.size ?: 0)
            val cs1Instructions = cs1Instructions
            if (cs1Instructions != null) {
                for (i in cs1Instructions.indices) {
                    data.writeShort(cs1Instructions[i].size)
                    for (j in cs1Instructions[i].indices) {
                        data.writeShort(cs1Instructions[i][j].toInt())
                    }
                }
            }

            if (type == 0) {
                data.writeShort(scrollHeight)
                data.writeBoolean(hide)
            }

            if (type == 1) {
                data.writeShort(0)
                data.writeByte(0)
            }

            if (type == 3) {
                data.writeBoolean(fill)
            }

            if (type == 4 || type == 1) {
                data.writeByte(textAlignH)
                data.writeByte(textAlignV)
                data.writeByte(textLineHeight)
                data.writeShort(textFont)
                data.writeBoolean(textShadow)
            }

            if (type == 4) {
                data.writeString(text)
                data.writeString(secondaryText)
            }

            if (type == 1 || type == 3 || type == 4) {
                data.writeInt(colour1)
            }

            if (type == 3 || type == 4) {
                data.writeInt(colour2)
                data.writeInt(mouseOverColour1)
                data.writeInt(mouseOverColour2)
            }

            if (type == 5) {
                data.writeInt(graphic)
                data.writeInt(secondaryGraphic)
            }

            if (type == 6) {
                data.writeShort(model)
                data.writeShort(secondaryModel)
                data.writeShort(modelAnim)
                data.writeShort(secondaryModelAnim)
                data.writeShort(modelZoom)
                data.writeShort(modelAngleX)
                data.writeShort(modelAngleY)
            }

            if (type == 8) {
                data.writeString(text)
            }

            if (buttonType == 2) {
                data.writeString(targetVerb)
                data.writeString(targetBase)
                data.writeShort(events)
            }

            if (buttonType == 1 || buttonType == 4 || buttonType == 5 || buttonType == 6) {
                data.writeString(buttonText)
            }
        }

    public fun encodeV3(unpacked: ComponentType, data: ByteBuf): Unit =
        with(unpacked) {
            data.writeByte(-1)
            data.writeByte(type)
            data.writeShort(clientCode)
            data.writeShort(x)
            data.writeShort(y)
            data.writeShort(width)
            data.writeShort(height)
            data.writeByte(widthMode)
            data.writeByte(heightMode)
            data.writeByte(xMode)
            data.writeByte(yMode)
            data.writeShort(layer)

            data.writeBoolean(hide)

            if (type == 0) {
                data.writeShort(scrollWidth)
                data.writeShort(scrollHeight)
                data.writeBoolean(noClickThrough)
            }

            if (type == 5) {
                data.writeInt(graphic)
                data.writeShort(angle2d)
                data.writeBoolean(tiling)
                data.writeByte(trans1)
                data.writeByte(outline)
                data.writeInt(graphicShadow)
                data.writeBoolean(vFlip)
                data.writeBoolean(hFlip)
            }

            if (type == 6) {
                data.writeShort(model)
                data.writeShort(modelX)
                data.writeShort(modelY)
                data.writeShort(modelAngleX)
                data.writeShort(modelAngleY)
                data.writeShort(modelAngleZ)
                data.writeShort(modelZoom)
                data.writeShort(modelAnim)
                data.writeBoolean(modelOrthog)
                data.writeShort(0)
                if (widthMode != 0) {
                    data.writeShort(modelObjWidth)
                }
                if (heightMode != 0) {
                    data.writeShort(0)
                }
            }

            if (type == 4) {
                data.writeShort(textFont)
                data.writeString(text)
                data.writeByte(textLineHeight)
                data.writeByte(textAlignH)
                data.writeByte(textAlignV)
                data.writeBoolean(textShadow)
                data.writeInt(colour1)
            }

            if (type == 3) {
                data.writeInt(colour1)
                data.writeBoolean(fill)
                data.writeByte(trans1)
            }

            if (type == 9) {
                data.writeByte(lineWid)
                data.writeInt(colour1)
                data.writeBoolean(lineDirection)
            }

            data.writeMedium(events)
            data.writeString(opBase)

            data.writeByte(op.size)
            for (i in op.indices) {
                data.writeString(op[i])
            }

            data.writeByte(dragDeadZone)
            data.writeByte(dragDeadTime)
            data.writeBoolean(draggableBehavior)
            data.writeString(targetVerb)

            encodeHook(onLoad, data,internalId)
            encodeHook(onMouseOver, data,internalId)
            encodeHook(onMouseLeave, data,internalId)
            encodeHook(onTargetLeave, data,internalId)
            encodeHook(onTargetEnter, data,internalId)
            encodeHook(onVarTransmit, data,internalId)
            encodeHook(onInvTransmit, data,internalId)
            encodeHook(onStatTransmit, data,internalId)
            encodeHook(onTimer, data,internalId)
            encodeHook(onOp, data,internalId)
            encodeHook(onMouseRepeat, data,internalId)
            encodeHook(onClick, data,internalId)
            encodeHook(onClickRepeat, data,internalId)
            encodeHook(onRelease, data,internalId)
            encodeHook(onHold, data,internalId)
            encodeHook(onDrag, data,internalId)
            encodeHook(onDragComplete, data,internalId)
            encodeHook(onScrollWheel, data,internalId)
            encodeHookTransmitList(onVarTransmitList, data)
            encodeHookTransmitList(onInvTransmitList, data)
            encodeHookTransmitList(onStatTransmitList, data)
        }

    public fun encodeHook(values: Array<Any>?, data: ByteBuf, packed : Int?) {
        data.writeByte(values?.size ?: 0)
        if (values == null) {
            return
        }
        for (i in values.indices) {
            var value = values[i]
            if (value is String && value.contains("component") && packed != null) {
                value = packed
            }
            when (value) {
                is Int -> {
                    data.writeByte(0)
                    data.writeInt(value)
                }

                is String -> {
                    data.writeByte(1)
                    data.writeString(value)
                }

                else -> error("Invalid value type: $value (${value::class.simpleName})")
            }
        }
    }

    public fun encodeHookTransmitList(values: IntArray?, data: ByteBuf) {
        data.writeByte(values?.size ?: 0)
        if (values == null) {
            return
        }
        for (i in values.indices) {
            data.writeInt(values[i])
        }
    }

}