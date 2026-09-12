package dev.openrune.seralizer

import dev.openrune.definition.type.ObjStackability
import dev.openrune.toml.model.TomlValue
import dev.openrune.toml.serialization.TomlFieldDecodeContext
import dev.openrune.toml.serialization.TomlFieldEncodeContext
import dev.openrune.toml.serialization.TomlFieldSerializer
import dev.openrune.toml.transcoding.TomlDecoder
import dev.openrune.toml.transcoding.TomlEncoder
import kotlin.reflect.KType

object ObjStackabilitySerializer : TomlFieldSerializer<ObjStackability> {

    override fun decodeField(
        decoder: TomlDecoder,
        targetType: KType,
        value: TomlValue?,
        decodeContext: TomlFieldDecodeContext,
    ): ObjStackability {
        return when (value) {
            null -> ObjStackability.Sometimes
            is TomlValue.Integer -> ObjStackability.fromId(value.value.toInt())
            is TomlValue.String ->
                ObjStackability.entries.firstOrNull {
                    it.name.equals(value.value, ignoreCase = true)
                } ?: error(
                    "Unknown stacks value '${value.value}'"
                )

            else ->
                error("stacks expected a string or integer, got ${value::class.simpleName}")
        }
    }

    override fun encodeField(
        encoder: TomlEncoder,
        value: ObjStackability,
        encodeContext: TomlFieldEncodeContext,
    ): TomlValue = TomlValue.String(value.name)
}
