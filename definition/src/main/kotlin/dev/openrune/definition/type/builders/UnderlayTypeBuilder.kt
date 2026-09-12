package dev.openrune.definition.type.builders

import dev.openrune.definition.type.*

/**
 * The mutable side of [UnderlayType]. Codecs decode into one of these and the packing tools edit
 * one — either fresh or via [UnderlayType.toBuilder] — then [build] produces the immutable
 * definition everything else reads.
 */
class UnderlayTypeBuilder(var id: Int = -1) {

    var rgb: Int = 0

    /**
     * Whether [build] recomputes the derived HSL fields via [UnderlayType.setHsl]. The codec's
     * decode sets this when it reads any opcode (mirroring the old read path, which called
     * `setHsl` after every opcode); a definition built from empty data keeps the all-zero
     * defaults, exactly as before.
     */
    var applyHsl: Boolean = false

    fun build(): UnderlayType {
        val type = UnderlayType(
            id = id,
            rgb = rgb,
        )
        if (applyHsl) {
            type.setHsl(rgb)
        }
        return type
    }

    companion object {
        /** Copies every field of [type] into a fresh builder. */
        fun from(type: UnderlayType): UnderlayTypeBuilder {
            val builder = UnderlayTypeBuilder(type.id)
            builder.rgb = type.rgb
            // Rebuilding recomputes HSL from rgb, so an edited rgb never carries stale HSL.
            builder.applyHsl = true
            return builder
        }
    }
}
