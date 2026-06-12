package dev.openrune.definition.dbtables

import dev.openrune.toml.model.TomlValue

/**
 * Packs and unpacks coordinate values using the same bit layout as [org.rsmod.map.CoordGrid].
 * Stored db cells always use the packed integer form.
 */
internal object CoordGridCodec {

    private const val MAP_SQUARE_LENGTH = 64

    private const val Z_BIT_COUNT = 14
    private const val X_BIT_COUNT = 14
    private const val LEVEL_BIT_COUNT = 2

    private const val Z_BIT_OFFSET = 0
    private const val X_BIT_OFFSET = Z_BIT_OFFSET + Z_BIT_COUNT
    private const val LEVEL_BIT_OFFSET = X_BIT_OFFSET + X_BIT_COUNT

    private const val Z_BIT_MASK = (1 shl Z_BIT_COUNT) - 1
    private const val X_BIT_MASK = (1 shl X_BIT_COUNT) - 1
    private const val LEVEL_BIT_MASK = (1 shl LEVEL_BIT_COUNT) - 1

    private val conventionalPattern = Regex("""^(\d+)_(\d+)_(\d+)_(\d+)_(\d+)$""")

    fun parse(value: TomlValue, context: String): Int = when (value) {
        is TomlValue.Integer -> value.value.toInt()
        is TomlValue.String -> parseConventional(value.value, context)
        is TomlValue.List -> parseList(value, context)
        is TomlValue.Map -> parseMap(value.properties, context)
        else -> throw IllegalArgumentException("$context: COORDGRID expected integer, string, array, or table")
    }

    fun pack(x: Int, z: Int, level: Int): Int {
        require(x in 0..X_BIT_MASK) { "x must be within [0..$X_BIT_MASK] (x=$x)" }
        require(z in 0..Z_BIT_MASK) { "z must be within [0..$Z_BIT_MASK] (z=$z)" }
        require(level in 0..LEVEL_BIT_MASK) { "level must be within [0..$LEVEL_BIT_MASK] (level=$level)" }
        return ((x and X_BIT_MASK) shl X_BIT_OFFSET) or
            ((z and Z_BIT_MASK) shl Z_BIT_OFFSET) or
            ((level and LEVEL_BIT_MASK) shl LEVEL_BIT_OFFSET)
    }

    fun pack(level: Int, mx: Int, mz: Int, lx: Int, lz: Int): Int {
        require(lx in 0 until MAP_SQUARE_LENGTH) { "lx must be within [0..${MAP_SQUARE_LENGTH - 1}] (lx=$lx)" }
        require(lz in 0 until MAP_SQUARE_LENGTH) { "lz must be within [0..${MAP_SQUARE_LENGTH - 1}] (lz=$lz)" }
        return pack(
            x = mx * MAP_SQUARE_LENGTH + lx,
            z = mz * MAP_SQUARE_LENGTH + lz,
            level = level,
        )
    }

    private fun parseConventional(raw: String, context: String): Int {
        val match = conventionalPattern.matchEntire(raw.trim())
            ?: throw IllegalArgumentException("$context: COORDGRID string must be level_mx_mz_lx_lz (e.g. 0_50_50_32_32)")
        val (level, mx, mz, lx, lz) = match.destructured
        return pack(level.toInt(), mx.toInt(), mz.toInt(), lx.toInt(), lz.toInt())
    }

    private fun parseList(value: TomlValue.List, context: String): Int {
        val elements = value.elements.map { requireInt(it, context) }
        return when (elements.size) {
            2 -> pack(elements[0], elements[1], 0)
            3 -> pack(elements[0], elements[1], elements[2])
            5 -> pack(elements[0], elements[1], elements[2], elements[3], elements[4])
            else -> throw IllegalArgumentException(
                "$context: COORDGRID array must be [x, z], [x, z, level], or [level, mx, mz, lx, lz]",
            )
        }
    }

    private fun parseMap(properties: Map<String, TomlValue>, context: String): Int {
        val level = properties["level"]?.let { requireInt(it, context) } ?: 0
        if (properties.containsKey("mx")) {
            val mx = requireInt(properties["mx"] ?: throw IllegalArgumentException("$context: COORDGRID table missing mx"), context)
            val mz = requireInt(properties["mz"] ?: throw IllegalArgumentException("$context: COORDGRID table missing mz"), context)
            val lx = requireInt(properties["lx"] ?: throw IllegalArgumentException("$context: COORDGRID table missing lx"), context)
            val lz = requireInt(properties["lz"] ?: throw IllegalArgumentException("$context: COORDGRID table missing lz"), context)
            return pack(level, mx, mz, lx, lz)
        }
        val x = requireInt(properties["x"] ?: throw IllegalArgumentException("$context: COORDGRID table missing x"), context)
        val z = requireInt(
            properties["z"] ?: properties["y"] ?: throw IllegalArgumentException("$context: COORDGRID table missing z"),
            context,
        )
        return pack(x, z, level)
    }

    private fun requireInt(value: TomlValue, context: String): Int = when (value) {
        is TomlValue.Integer -> value.value.toInt()
        else -> throw IllegalArgumentException("$context: COORDGRID expected integer component")
    }

    fun unpack(packed: Int): Triple<Int, Int, Int> {
        val x = (packed shr X_BIT_OFFSET) and X_BIT_MASK
        val z = (packed shr Z_BIT_OFFSET) and Z_BIT_MASK
        val level = (packed shr LEVEL_BIT_OFFSET) and LEVEL_BIT_MASK
        return Triple(x, z, level)
    }

    fun formatExport(packed: Int): String {
        val (x, z, level) = unpack(packed)
        return if (level == 0) "[$x, $z]" else "[$x, $z, $level]"
    }
}
