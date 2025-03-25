package dev.openrune.definition.serialization

import dev.openrune.definition.util.Type
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object TypeSerializer : KSerializer<Type> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("Type", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Type) {
        encoder.encodeString(value.name)
    }

    override fun deserialize(decoder: Decoder): Type {
        return Type.valueOf(decoder.decodeString().uppercase())
    }
}
