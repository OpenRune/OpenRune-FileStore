package dev.openrune.definition.opcode

import dev.openrune.definition.util.readNullableLargeSmart
import io.netty.buffer.ByteBuf
import dev.openrune.definition.util.readString
import dev.openrune.definition.util.readShortSmart
import dev.openrune.definition.util.writeNullableLargeSmartCorrect
import dev.openrune.definition.util.writePrefixedString
import dev.openrune.definition.util.writeShortSmart
import dev.openrune.definition.util.writeSmart
import dev.openrune.definition.util.writeString
import kotlin.reflect.KClass


interface BufferSerializer<T> {
    fun read(buf: ByteBuf): T
    fun write(buf: ByteBuf, value: T)
}

sealed class OpcodeType<T>(
    private val serializer: BufferSerializer<T>
) : BufferSerializer<T> by serializer {


    inline fun <reified T : Any> dataClassType(): OpcodeType<T> = DataClassJavaOpcodeType(T::class.java)
    inline fun <reified E : Enum<E>> enumType(): OpcodeType<E> = EnumOpcodeType(E::class)
    class DataClassJavaOpcodeType<T : Any>(private val clazz: Class<T>) : OpcodeType<T>(DataClassJavaSerializer(clazz))

    class DataClassJavaSerializer<T : Any>(private val clazz: Class<T>) : BufferSerializer<T> {

        override fun read(buf: ByteBuf): T = deserializeDataClassJava(buf, clazz)

        override fun write(buf: ByteBuf, value: T) = serializeDataClassJava(buf, value)

        private fun serializeDataClassJava(buf: ByteBuf, obj: Any) {
            val fields = clazz.declaredFields.sortedBy { it.name }
            for (field in fields) {
                field.isAccessible = true
                val value = field.get(obj)
                when (value) {
                    is Int -> buf.writeInt(value)
                    is String -> buf.writeString(value)
                    else -> throw IllegalArgumentException("Unsupported field type: ${field.type}")
                }
            }
        }

        private fun deserializeDataClassJava(buf: ByteBuf, clazz: Class<T>): T {
            val ctor = clazz.declaredConstructors.first()
            val paramTypes = ctor.parameterTypes
            val args = Array<Any?>(paramTypes.size) { i ->
                when (paramTypes[i]) {
                    Int::class.javaPrimitiveType, java.lang.Integer::class.java -> buf.readInt()
                    String::class.java -> buf.readString()
                    else -> throw IllegalArgumentException("Unsupported parameter type: ${paramTypes[i]}")
                }
            }
            ctor.isAccessible = true
            return ctor.newInstance(*args) as T
        }
    }

    class EnumOpcodeType<E : Enum<E>>(private val enumClass: KClass<E>) : OpcodeType<E>(object : BufferSerializer<E> {
        override fun read(buf: ByteBuf): E {
            val str = buf.readString()
            return enumClass.java.enumConstants.first { it.name == str }
        }

        override fun write(buf: ByteBuf, value: E) {
            buf.writeString(value.name)
        }
    })

    data object STRING : OpcodeType<String>(object : BufferSerializer<String> {
        override fun read(buf: ByteBuf): String = buf.readString()

        override fun write(buf: ByteBuf, value: String) {
            buf.writeString(value)
        }
    })

    data object PREFIXED_STRING : OpcodeType<String>(object : BufferSerializer<String> {
        override fun read(buf: ByteBuf): String = buf.readString()

        override fun write(buf: ByteBuf, value: String) {
            buf.writePrefixedString(value)
        }
    })


    data object NULLABLE_LARGE_SMART : OpcodeType<Int>(object : BufferSerializer<Int> {
        override fun read(buf: ByteBuf): Int = buf.readNullableLargeSmart()

        override fun write(buf: ByteBuf, value: Int) {
            buf.writeNullableLargeSmartCorrect(value)
        }
    })

    data object UBYTE : OpcodeType<Int>(object : BufferSerializer<Int> {
        override fun read(buf: ByteBuf): Int = buf.readUnsignedByte().toInt()
        override fun write(buf: ByteBuf, value: Int) {
            buf.writeByte(value)
        }
    })

    data object BYTE : OpcodeType<Int>(object : BufferSerializer<Int> {
        override fun read(buf: ByteBuf): Int = buf.readByte().toInt()
        override fun write(buf: ByteBuf, value: Int) {
            buf.writeByte(value)
        }
    })

    data object SHORT : OpcodeType<Int>(object : BufferSerializer<Int> {
        override fun read(buf: ByteBuf): Int = buf.readShort().toInt()
        override fun write(buf: ByteBuf, value: Int) {
            buf.writeShort(value)
        }
    })

    data object USHORT : OpcodeType<Int>(object : BufferSerializer<Int> {
        override fun read(buf: ByteBuf): Int = buf.readUnsignedShort()
        override fun write(buf: ByteBuf, value: Int) {
            buf.writeShort(value)
        }
    })

    data object INT : OpcodeType<Int>(object : BufferSerializer<Int> {
        override fun read(buf: ByteBuf): Int = buf.readInt()
        override fun write(buf: ByteBuf, value: Int) {
            buf.writeInt(value)
        }
    })

    data object DOUBLE : OpcodeType<Double>(object : BufferSerializer<Double> {
        override fun read(buf: ByteBuf): Double = buf.readDouble()
        override fun write(buf: ByteBuf, value: Double) {
            buf.writeDouble(value)
        }
    })

    data object BOOLEAN : OpcodeType<Boolean>(object : BufferSerializer<Boolean> {
        override fun read(buf: ByteBuf): Boolean = buf.readBoolean()
        override fun write(buf: ByteBuf, value: Boolean) {
            buf.writeBoolean(value)
        }
    })

    data object UMEDIUM : OpcodeType<Int>(object : BufferSerializer<Int> {
        override fun read(buf: ByteBuf): Int = buf.readUnsignedMedium()

        override fun write(buf: ByteBuf, value: Int) {
            buf.writeMedium(value)
        }
    })

    data object SHORT_SMART : OpcodeType<Int>(object : BufferSerializer<Int> {
        override fun read(buf: ByteBuf): Int = buf.readShortSmart()

        override fun write(buf: ByteBuf, value: Int) {
            buf.writeShortSmart(value)
        }
    })

    companion object {
        @Suppress("UNCHECKED_CAST")
        inline fun <reified T> get(): OpcodeType<T>? = when (T::class) {
            String::class -> STRING as OpcodeType<T>
            Int::class -> INT as OpcodeType<T> // default to INT for Int
            Double::class -> DOUBLE as OpcodeType<T>
            Boolean::class -> BOOLEAN as OpcodeType<T>
            else -> null
        }
    }
}