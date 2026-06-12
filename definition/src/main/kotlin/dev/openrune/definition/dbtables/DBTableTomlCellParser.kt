package dev.openrune.definition.dbtables

import dev.openrune.definition.util.BaseVarType
import dev.openrune.definition.util.CacheVarLiteral
import dev.openrune.toml.model.TomlValue

internal object DBTableTomlCellParser {

    private fun cellError(context: String, message: String): Nothing =
        throw IllegalArgumentException("$context: $message")

    fun parseCellValues(value: TomlValue, types: Array<CacheVarLiteral>, context: String): Array<Any> =
        when (value) {
            is TomlValue.List -> {
                if (types.size == 1 && types[0].name == "COORDGRID") {
                    arrayOf(CoordGridCodec.parse(value, context))
                } else {
                    flattenList(value, types, context)
                }
            }
            else -> arrayOf(parseCellValue(value, types.firstOrNull() ?: CacheVarLiteral.INT, context))
        }

    private fun flattenList(value: TomlValue.List, types: Array<CacheVarLiteral>, context: String): Array<Any> {
        if (value.elements.isEmpty()) return emptyArray()
        if (value.elements.all { it is TomlValue.List }) {
            return value.elements.flatMapIndexed { pairIndex, inner ->
                (inner as TomlValue.List).elements.mapIndexed { i, element ->
                    val typeIndex = pairIndex * inner.elements.size + i
                    val type = types.getOrNull(typeIndex) ?: types.lastOrNull() ?: CacheVarLiteral.INT
                    parseCellValue(element, type, "$context[$typeIndex]")
                }
            }.toTypedArray()
        }
        return value.elements.mapIndexed { index, element ->
            val type = types.getOrNull(index) ?: types.lastOrNull() ?: CacheVarLiteral.INT
            parseCellValue(element, type, "$context[$index]")
        }.toTypedArray()
    }

    private fun parseCellValue(value: TomlValue, type: CacheVarLiteral, context: String): Any {
        validateTomlShape(value, type, context)
        return when (type.name) {
            "COORDGRID" -> CoordGridCodec.parse(value, context)
            "STRING" -> (value as TomlValue.String).value
            "INT" -> (value as TomlValue.Integer).value.toInt()
            "BOOLEAN" -> when (value) {
                is TomlValue.Bool -> value.value
                is TomlValue.Integer -> value.value.toInt() != 0
                else -> cellError(context, "BOOLEAN expected bool or 0/1 integer")
            }
            else -> when (type.baseType) {
                BaseVarType.STRING -> (value as TomlValue.String).value
                BaseVarType.INTEGER -> parseIntegerCell(value, type, context)
                BaseVarType.LONG -> (value as TomlValue.Integer).value.toLong()
                BaseVarType.ARRAY -> cellError(context, "array column type ${type.name} is not supported in TOML rows")
            }
        }
    }

    private fun parseIntegerCell(value: TomlValue, type: CacheVarLiteral, context: String): Any = when (value) {
        is TomlValue.Integer -> value.value.toInt()
        is TomlValue.String -> DBTableCellCodec.decodeString(value.value, type)
        else -> cellError(context, "${type.name} expected integer or RSCM string")
    }

    private fun validateTomlShape(value: TomlValue, type: CacheVarLiteral, context: String) {
        when (type.name) {
            "STRING" -> requireString(value, context, type.name)
            "INT" -> requireInteger(value, context, type.name)
            "BOOLEAN" -> requireBoolean(value, context)
            "COORDGRID" -> requireCoordShape(value, context)
            else -> when (type.baseType) {
                BaseVarType.STRING -> requireString(value, context, type.name)
                BaseVarType.INTEGER -> {
                    if (DBTableCellCodec.usesRscmMapping(type)) {
                        requireRscmRef(value, context, type.name)
                    } else {
                        requireInteger(value, context, type.name)
                    }
                }
                BaseVarType.LONG -> requireInteger(value, context, type.name)
                BaseVarType.ARRAY -> cellError(context, "array column type ${type.name} is not supported in TOML rows")
            }
        }
    }

    private fun requireString(value: TomlValue, context: String, typeName: String) {
        if (value !is TomlValue.String) {
            cellError(context, "$typeName column requires a string value, got ${tomlKind(value)}")
        }
    }

    private fun requireInteger(value: TomlValue, context: String, typeName: String) {
        if (value !is TomlValue.Integer) {
            cellError(context, "$typeName column requires an integer value, got ${tomlKind(value)}")
        }
    }

    private fun requireBoolean(value: TomlValue, context: String) {
        if (value !is TomlValue.Bool && value !is TomlValue.Integer) {
            cellError(context, "BOOLEAN column requires a boolean or 0/1 integer, got ${tomlKind(value)}")
        }
    }

    private fun requireRscmRef(value: TomlValue, context: String, typeName: String) {
        if (value !is TomlValue.Integer && value !is TomlValue.String) {
            cellError(context, "$typeName column requires an integer id or RSCM string, got ${tomlKind(value)}")
        }
    }

    private fun requireCoordShape(value: TomlValue, context: String) {
        val valid = when (value) {
            is TomlValue.Integer, is TomlValue.String, is TomlValue.Map -> true
            is TomlValue.List -> value.elements.all { it is TomlValue.Integer }
            else -> false
        }
        if (!valid) {
            cellError(
                context,
                "COORDGRID column requires packed int, level_mx_mz_lx_lz string, [x, z], [x, z, level], [level, mx, mz, lx, lz], or {x,z,level} table",
            )
        }
    }

    private fun tomlKind(value: TomlValue): String = when (value) {
        is TomlValue.String -> "string"
        is TomlValue.Integer -> "integer"
        is TomlValue.Bool -> "boolean"
        is TomlValue.List -> "array"
        is TomlValue.Map -> "table"
        else -> value::class.simpleName ?: "unknown"
    }

    fun normalizeColumnTypeName(name: String): String = when (name.uppercase()) {
        "COORD" -> "COORDGRID"
        else -> name.uppercase()
    }
}
