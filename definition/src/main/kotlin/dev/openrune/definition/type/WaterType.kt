package dev.openrune.definition.type

import dev.openrune.definition.Definition
import dev.openrune.definition.Rs2Opcode
import dev.openrune.definition.Rs2OpcodeOrdered

data class WaterType(
    override var id: Int = -1,
    val values: MutableMap<Int, List<Number>> = LinkedHashMap(),
    override val opcodeOrder: MutableList<Rs2Opcode> = mutableListOf(),
) : Definition, Rs2OpcodeOrdered {

    private fun int(opcode: Int): Int? = values[opcode]?.firstOrNull()?.toInt()
    private fun float(opcode: Int): Float? = values[opcode]?.firstOrNull()?.toFloat()

    var baseRgba: Int?
        get() = int(12)
        set(value) = set(12, value)

    val foamMaterial: Int? get() = int(9)
    val waterDepthFoam: Int? get() = int(14)
    val reflectionStrength: Int? get() = int(5)
    val specularShininess: Int? get() = int(25)
    val specularFactor: Int? get() = int(27)
    val fresnelBias: Float? get() = float(83)
    val normalMapMaterial1: Int? get() = int(29)
    val normalMapMaterial2: Int? get() = int(30)
    val normalMapMaterial3: Int? get() = int(31)
    val emissiveMapMaterial: Int? get() = int(86)
    val emissiveRgb: Int? get() = int(89)
    val stillWaterNormalStrength: Float? get() = float(81)
    val flowNoise: Float? get() = float(82)
    val overrideDefaultWaterType: Boolean get() = int(85) == 1
    val extinctionOpaqueWaterColour: Int? get() = int(98)
    val extinctionVisibilityMetres: Float? get() = float(99)

    private fun set(opcode: Int, value: Int?) {
        if (value == null) values.remove(opcode) else values[opcode] = listOf(value)
    }

    fun normalMapEnabled(target: Int): Boolean? =
        values[WaterField.normalMapOpcode(target, 0)]?.firstOrNull()?.toInt()?.let { it == 1 }
}

enum class WaterField {
    BYTE,
    SHORT,
    MEDIUM,
    INT,
    FLOAT,
    SHORT_X2,
    SHORT_X3,
    FLOAT_X2,
    FLOAT_X3,
    ;

    companion object {
        private const val NORMAL_MAP_FIRST = 33
        private const val NORMAL_MAP_TARGETS = 6
        private const val NORMAL_MAP_OPS = 8
        private const val NORMAL_MAP_LAST = NORMAL_MAP_FIRST + NORMAL_MAP_TARGETS * NORMAL_MAP_OPS - 1

        fun normalMapOpcode(target: Int, op: Int) = NORMAL_MAP_FIRST + target * NORMAL_MAP_OPS + op

        private val BY_OPCODE: Map<Int, WaterField> = buildMap {
            listOf(18, 19, 24, 85, 92, 96).forEach { put(it, BYTE) }
            listOf(
                1, 2, 3, 4, 5, 8, 9, 10, 11, 13, 14, 16, 17, 20, 21, 22, 23, 25, 27, 28,
                29, 30, 31, 32, 86, 87,
            ).forEach { put(it, SHORT) }
            put(6, MEDIUM)
            listOf(12, 15, 89, 98).forEach { put(it, INT) }
            listOf(
                81, 82, 83, 84, 90, 91, 93, 94, 95, 99,
                100, 101, 102, 103, 104, 105, 106, 107, 108,
            ).forEach { put(it, FLOAT) }
            put(7, SHORT_X2)
            put(26, SHORT_X3)
            put(88, FLOAT_X2)
            put(97, FLOAT_X3)

            for (target in 0 until NORMAL_MAP_TARGETS) {
                for (op in 0 until NORMAL_MAP_OPS) {
                    put(
                        normalMapOpcode(target, op),
                        when (op) {
                            0 -> BYTE
                            4, 5 -> FLOAT_X2
                            else -> FLOAT
                        },
                    )
                }
            }
        }

        fun of(opcode: Int): WaterField? = BY_OPCODE[opcode]

        fun isNormalMap(opcode: Int) = opcode in NORMAL_MAP_FIRST..NORMAL_MAP_LAST
    }
}
