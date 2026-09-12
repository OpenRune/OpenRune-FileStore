package dev.openrune.definition.type

import dev.openrune.definition.type.builders.ItemTypeBuilder

import dev.openrune.toml.rsconfig.RsTableHeaders
import dev.openrune.toml.serialization.TomlField
import dev.openrune.definition.Definition
import dev.openrune.definition.EntityOpsBuilder
import dev.openrune.definition.EntityOpsDefinition
import dev.openrune.definition.Parameterized
import dev.openrune.definition.Recolourable
import dev.openrune.seralizer.ObjStackabilitySerializer
import dev.openrune.seralizer.ItemTypeOptionsTableHook
import dev.openrune.seralizer.ParamSerializer

/** The op set every item starts with; shared because [EntityOpsDefinition] is immutable. */
val DEFAULT_ITEM_OPTIONS: EntityOpsDefinition = EntityOpsBuilder().op(2, "Take").build()

/**
 * A loaded item definition. Immutable apart from [id] and the handful of fields the TOML options
 * hook assigns after construction ([options], [equipSlot], [appearanceOverride1],
 * [appearanceOverride2]; [interfaceOptions] is filled in place). Decoding and packing build one
 * through [ItemTypeBuilder].
 */
@RsTableHeaders(
    "item",
    rowPostDecode = ItemTypeOptionsTableHook::class,
)
data class ItemType(
    override var id: Int = -1,
    val name: String = "null",
    val examine: String = "null",
    override val originalColours: List<Int>? = null,
    override val modifiedColours: List<Int>? = null,
    override val originalTextureColours: List<Int>? = null,
    override val modifiedTextureColours: List<Int>? = null,
    // Stays MutableMap: ParamSerializer's declared type argument must match the parameter type.
    @param:TomlField(serializer = ParamSerializer::class)
    override val params: MutableMap<Int, Any>? = null,
    val resizeX: Int = 128,
    val resizeY: Int = 128,
    val resizeZ: Int = 128,
    val xan2d: Int = 0,
    val category: Int = -1,
    val yan2d: Int = 0,
    val zan2d: Int = 0,
    // var: the TOML options hook assigns these after construction.
    var equipSlot: Int = -1,
    var appearanceOverride1: Int = -1,
    var appearanceOverride2: Int = -1,
    val weight: Double = 0.0,
    val cost: Int = 1,
    val stockMarket: Boolean = false,
    val tradeable: Boolean = true,
    @param:TomlField(serializer = ObjStackabilitySerializer::class)
    val stacks: ObjStackability = ObjStackability.Sometimes,
    val inventoryModel: Int = 0,
    val members: Boolean = false,
    val zoom2d: Int = 2000,
    val xOffset2d: Int = 0,
    val yOffset2d: Int = 0,
    val ambient: Int = 0,
    val contrast: Int = 0,
    val countCo: List<Int>? = null,
    val countObj: List<Int>? = null,
    // var: the TOML options hook replaces it; immutable and shared, every stock item points at
    // the same default "Take" op set.
    var options : EntityOpsDefinition = DEFAULT_ITEM_OPTIONS,
    // The hook fills slots in place, so this stays a mutable list by design.
    val interfaceOptions: MutableList<String?> = mutableListOf(null, null, null, null, "Drop"),
    val maleModel0: Int = -1,
    val maleModel1: Int = -1,
    val maleModel2: Int = -1,
    val maleOffset: Int = 0,
    val maleHeadModel0: Int = -1,
    val maleHeadModel1: Int = -1,
    val femaleModel0: Int = -1,
    val femaleModel1: Int = -1,
    val femaleModel2: Int = -1,
    val femaleOffset: Int = 0,
    val femaleHeadModel0: Int = -1,
    val femaleHeadModel1: Int = -1,
    val noteLinkId: Int = -1,
    val noteTemplateId: Int = -1,
    val teamCape: Int = 0,
    val dropOptionIndex: Int = -2,
    val unnotedId: Int = -1,
    val notedId: Int = -1,
    val placeholderLink: Int = -1,
    val placeholderTemplate: Int = -1,
    val subops: Array<Array<String?>?>? = null,

    ) : Definition, Recolourable, Parameterized {

    // In the body so it stays out of equals/hashCode/toString/copy. Used by the r718 codec.
    private var extraProperties: MutableMap<String, Any?>? = null

    override val extra: MutableMap<String, Any?>
        get() = extraProperties ?: LinkedHashMap<String, Any?>(8).also { extraProperties = it }

    /** A mutable copy of this definition, for tools that need to edit and re-pack it. */
    fun toBuilder(): ItemTypeBuilder = ItemTypeBuilder.from(this)

    val stackable: Boolean
        get() = stacks == ObjStackability.Always || noteTemplateId > 0

    val noted: Boolean
        get() = noteTemplateId > 0

    /**
     * Whether or not the object is a placeholder.
     */
    val isPlaceholder
        get() = placeholderTemplate > 0 && placeholderLink > 0
}

public enum class ObjStackability(public val id: Int) {
    Sometimes(0),
    Always(1),
    Never(2);

    public companion object {
        public fun fromId(id: Int): ObjStackability =
            entries.firstOrNull { it.id == id } ?: Sometimes
    }
}
