package dev.openrune.definition.opcode.impl

import dev.openrune.definition.opcode.DefinitionOpcode
import io.netty.buffer.ByteBuf
import kotlin.reflect.KMutableProperty1

/**
 * Creates an opcode for reading/writing color pairs (originalColours, modifiedColours).
 * Format: opcode, length byte, then pairs of shorts (original, modified)
 * Example: opcode 40 for colors
 */
fun <T> DefinitionOpcodeColours(
    opcode: Int,
    originalColoursGetter: (T) -> MutableList<Int>?,
    modifiedColoursGetter: (T) -> MutableList<Int>?,
    originalColoursSetter: (T, MutableList<Int>) -> Unit,
    modifiedColoursSetter: (T, MutableList<Int>) -> Unit
): DefinitionOpcode<T> = DefinitionOpcode(
    opcode = opcode,
    decode = { buf, def, _ ->
        val length = buf.readUnsignedByte().toInt()
        val originalColours = MutableList(length) { -1 }
        val modifiedColours = MutableList(length) { -1 }
        for (count in 0 until length) {
            originalColours[count] = buf.readShort().toInt().toShort().toInt()
            modifiedColours[count] = buf.readShort().toInt().toShort().toInt()
        }
        originalColoursSetter(def, originalColours)
        modifiedColoursSetter(def, modifiedColours)
    },
    encode = { buf, def ->
        val originalColours = originalColoursGetter(def)!!
        val modifiedColours = modifiedColoursGetter(def)!!
        buf.writeByte(originalColours.size)
        for (i in originalColours.indices) {
            buf.writeShort(originalColours[i])
            buf.writeShort(modifiedColours[i])
        }
    },
    shouldEncode = { def ->
        originalColoursGetter(def) != null && modifiedColoursGetter(def) != null
    }
)

/**
 * Creates an opcode for reading/writing color pairs using properties.
 */
fun <T> DefinitionOpcodeColours(
    opcode: Int,
    originalColours: KMutableProperty1<T, MutableList<Int>?>,
    modifiedColours: KMutableProperty1<T, MutableList<Int>?>
): DefinitionOpcode<T> = DefinitionOpcodeColours(
    opcode = opcode,
    originalColoursGetter = { originalColours.get(it) },
    modifiedColoursGetter = { modifiedColours.get(it) },
    originalColoursSetter = { obj, value -> originalColours.set(obj, value) },
    modifiedColoursSetter = { obj, value -> modifiedColours.set(obj, value) }
)

/**
 * Creates an opcode for reading/writing texture color pairs (originalTextureColours, modifiedTextureColours).
 * Format: opcode, length byte, then pairs of shorts (original, modified)
 * Example: opcode 41 for textures
 */
fun <T> DefinitionOpcodeTextures(
    opcode: Int,
    originalTextureColoursGetter: (T) -> MutableList<Int>?,
    modifiedTextureColoursGetter: (T) -> MutableList<Int>?,
    originalTextureColoursSetter: (T, MutableList<Int>) -> Unit,
    modifiedTextureColoursSetter: (T, MutableList<Int>) -> Unit
): DefinitionOpcode<T> = DefinitionOpcode(
    opcode = opcode,
    decode = { buf, def, _ ->
        val length = buf.readUnsignedByte().toInt()
        val originalTextureColours = MutableList(length) { -1 }
        val modifiedTextureColours = MutableList(length) { -1 }
        for (count in 0 until length) {
            originalTextureColours[count] = buf.readShort().toInt().toShort().toInt()
            modifiedTextureColours[count] = buf.readShort().toInt().toShort().toInt()
        }
        originalTextureColoursSetter(def, originalTextureColours)
        modifiedTextureColoursSetter(def, modifiedTextureColours)
    },
    encode = { buf, def ->
        val originalTextureColours = originalTextureColoursGetter(def)!!
        val modifiedTextureColours = modifiedTextureColoursGetter(def)!!
        buf.writeByte(originalTextureColours.size)
        for (i in originalTextureColours.indices) {
            buf.writeShort(originalTextureColours[i])
            buf.writeShort(modifiedTextureColours[i])
        }
    },
    shouldEncode = { def ->
        originalTextureColoursGetter(def) != null && modifiedTextureColoursGetter(def) != null
    }
)

/**
 * Creates an opcode for reading/writing texture color pairs using properties.
 */
fun <T> DefinitionOpcodeTextures(
    opcode: Int,
    originalTextureColours: KMutableProperty1<T, MutableList<Int>?>,
    modifiedTextureColours: KMutableProperty1<T, MutableList<Int>?>
): DefinitionOpcode<T> = DefinitionOpcodeTextures(
    opcode = opcode,
    originalTextureColoursGetter = { originalTextureColours.get(it) },
    modifiedTextureColoursGetter = { modifiedTextureColours.get(it) },
    originalTextureColoursSetter = { obj, value -> originalTextureColours.set(obj, value) },
    modifiedTextureColoursSetter = { obj, value -> modifiedTextureColours.set(obj, value) }
)

