package dev.openrune.definition.codec.new

import dev.openrune.definition.opcode.DefinitionOpcode
import dev.openrune.definition.opcode.IgnoreOpcode
import dev.openrune.definition.opcode.OpcodeDefinitionCodec
import dev.openrune.definition.opcode.OpcodeList
import dev.openrune.definition.opcode.OpcodeType
import dev.openrune.definition.opcode.impl.DefinitionOpcodeListActions
import dev.openrune.definition.type.AreaType

class AreaCodecNew : OpcodeDefinitionCodec<AreaType>() {

    override val definitionCodec = OpcodeList<AreaType>().apply {
        add(DefinitionOpcode(1, OpcodeType.NULLABLE_LARGE_SMART, AreaType::sprite1))
        add(DefinitionOpcode(2, OpcodeType.NULLABLE_LARGE_SMART, AreaType::sprite2))
        add(DefinitionOpcode(3, OpcodeType.STRING, AreaType::name))
        add(DefinitionOpcode(4, OpcodeType.UMEDIUM, AreaType::fontColor))
        add(IgnoreOpcode(5, OpcodeType.UMEDIUM))
        add(DefinitionOpcode(6, OpcodeType.UBYTE, AreaType::textSize))

        add(DefinitionOpcode(opcode = 7,
            decode = { buf, def, _ ->
                val flags = buf.readUnsignedByte().toInt()
                def.renderOnWorldMap = (flags and 1) != 0
                def.renderOnMinimap = (flags and 2) != 0
            },
            encode = { buf, def ->
                var flags = 0
                if (def.renderOnWorldMap) flags = flags or 1
                if (def.renderOnMinimap) flags = flags or 2
                buf.writeByte(flags)
            },
            shouldEncode = { true }
        ))

        add(IgnoreOpcode(8, OpcodeType.UBYTE))

        addAll(DefinitionOpcodeListActions(10..14, AreaType::options))

        add(DefinitionOpcode(opcode = 15,
            decode = { buf, def, _ ->
                val length = buf.readUnsignedByte().toInt()

                def.field1933 = MutableList(length * 2) {
                    buf.readShort().toInt()
                }

                buf.readInt()

                val subLength = buf.readUnsignedByte().toInt()
                def.field1930 = MutableList(subLength) {
                    buf.readInt()
                }

                def.field1948 = MutableList(length) {
                    buf.readByte().toInt()
                }
            },
            encode = { buf, def ->
                val field1933 = def.field1933 ?: error("AreaType.field1933 is null but opcode 15 was selected for encoding.")

                val length = when {
                    def.field1948.isNotEmpty() -> def.field1948.size
                    else -> field1933.size / 2
                }

                buf.writeByte(length)
                for (i in 0 until (length * 2)) {
                    buf.writeShort(field1933.getOrNull(i) ?: 0)
                }

                buf.writeInt(0)

                val subLength = def.field1930.size
                buf.writeByte(subLength)
                for (i in 0 until subLength) {
                    buf.writeInt(def.field1930[i])
                }

                for (i in 0 until length) {
                    buf.writeByte(def.field1948.getOrNull(i) ?: 0)
                }
            },
            shouldEncode = { def ->
                def.field1933 != null && (def.field1933!!.isNotEmpty() || def.field1930.isNotEmpty() || def.field1948.isNotEmpty())
            })
        )

        add(IgnoreOpcode(16, OpcodeType.BYTE))
        add(DefinitionOpcode(17, OpcodeType.STRING, AreaType::menuTargetName))
        add(IgnoreOpcode(18, OpcodeType.NULLABLE_LARGE_SMART))
        add(DefinitionOpcode(19, OpcodeType.USHORT, AreaType::category))
        add(IgnoreOpcode(21, OpcodeType.INT))
        add(IgnoreOpcode(22, OpcodeType.INT))
        add(IgnoreOpcode(23, OpcodeType.UMEDIUM))

        add(IgnoreOpcode(24) { buf ->
            buf.readShort()
            buf.readShort()
        })

        add(IgnoreOpcode(25, OpcodeType.NULLABLE_LARGE_SMART))
        add(IgnoreOpcode(28, OpcodeType.BYTE))
        add(DefinitionOpcode(29, OpcodeType.UBYTE, AreaType::horizontalAlignment))
        add(DefinitionOpcode(30, OpcodeType.UBYTE, AreaType::verticalAlignment))
    }


    override fun createDefinition() = AreaType()
}