package dev.openrune.seralizer

import dev.openrune.toml.model.TomlValue
import dev.openrune.toml.serialization.TomlFieldDecodeContext
import dev.openrune.toml.serialization.TomlFieldEncodeContext
import dev.openrune.toml.serialization.TomlFieldSerializer
import dev.openrune.toml.transcoding.TomlDecoder
import dev.openrune.toml.transcoding.TomlEncoder
import dev.openrune.toml.transcoding.encode
import kotlin.reflect.KType

object ParamSerializer : TomlFieldSerializer<MutableMap<Int, Any>> {

    override fun decodeField(
        decoder: TomlDecoder,
        targetType: KType,
        value: TomlValue?,
        decodeContext: TomlFieldDecodeContext,
    ): MutableMap<Int, Any> {
        val paramsTable = (value as? TomlValue.Map)?.properties ?: emptyMap()
        val out = LinkedHashMap<Int, Any>(paramsTable.size)
        for ((key, cell) in paramsTable) {
            val id = key.toIntOrNull()
                ?: error("params key '$key' must be an integer param id")
            out[id] = coerceParamValue(cell)
        }
        return out
    }

    override fun encodeField(
        encoder: TomlEncoder,
        value: MutableMap<Int, Any>,
        encodeContext: TomlFieldEncodeContext,
    ): TomlValue {
        val props = value.entries.associateTo(LinkedHashMap()) { (id, v) ->
            id.toString() to encoder.encode(v)
        }
        return TomlValue.Map(props)
    }

    private fun coerceParamValue(v: TomlValue): Any =
        when (v) {
            is TomlValue.Bool -> if (v.value) 1 else 0
            is TomlValue.Integer -> v.value.toInt()
            is TomlValue.String -> v.value
            else -> ""
        }
}
