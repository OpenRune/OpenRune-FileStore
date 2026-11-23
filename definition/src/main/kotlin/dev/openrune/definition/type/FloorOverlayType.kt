package dev.openrune.definition.type

import dev.openrune.definition.Definition
import dev.openrune.definition.Rs2Opcode
import dev.openrune.definition.Rs2OpcodeOrdered

data class FloorOverlayType(
    override var id: Int = -1,
    var colour: Int = 0,
    var material: Int? = null,
    var materialWide: Boolean = false,
    var overlay: Boolean = false,
    var occlude: Boolean = true,
    var debugName: String? = null,
    var mapColour: Int? = null,
    var unknown8: Boolean = false,
    var textureScale: Int? = null,
    var hardShadow: Boolean = true,
    var priority: Int? = null,
    var smoothEdges: Boolean = false,
    var waterFogColour: Int? = null,
    var waterFogScale: Int? = null,
    var unknown15: Int? = null,
    var waterFogOffset: Int? = null,
    var waterFogUnknownA: Int? = null,
    var waterFogUnknownB: Int? = null,
    var waterFogUnknownC: Int? = null,
    override val opcodeOrder: MutableList<Rs2Opcode> = mutableListOf(),
) : Definition, Rs2OpcodeOrdered
