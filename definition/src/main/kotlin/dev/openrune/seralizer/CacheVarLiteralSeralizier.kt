package dev.openrune.seralizer

import dev.openrune.toml.model.TomlValue
import dev.openrune.toml.serialization.TomlFieldDecodeContext
import dev.openrune.toml.serialization.TomlFieldEncodeContext
import dev.openrune.toml.serialization.TomlFieldSerializer
import dev.openrune.toml.transcoding.TomlDecoder
import dev.openrune.toml.transcoding.TomlEncoder
import dev.openrune.definition.util.CacheVarLiteral
import kotlin.reflect.KType

object CacheVarLiteralSeralizier : TomlFieldSerializer<CacheVarLiteral> {

    override fun decodeField(
        decoder: TomlDecoder,
        targetType: KType,
        value: TomlValue?,
        decodeContext: TomlFieldDecodeContext,
    ): CacheVarLiteral {
        val v = requireNotNull(value) { "expected TOML value for ${decodeContext.parameterName}" }
        return when (v) {
            is TomlValue.String -> CacheVarLiteral.byName(v.value)
            is TomlValue.Integer -> CacheVarLiteral[v.value.toInt()]
            else -> error("CacheVarLiteral expected a TOML string or integer, got ${v::class.simpleName}")
        }
    }

    override fun encodeField(
        encoder: TomlEncoder,
        value: CacheVarLiteral,
        encodeContext: TomlFieldEncodeContext,
    ): TomlValue =
        TomlValue.String(value.name)
}
