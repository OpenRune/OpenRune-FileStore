package dev.openrune.definition.type

import dev.openrune.definition.type.builders.NpcTypeBuilder

import dev.openrune.toml.rsconfig.RsTableHeaders
import dev.openrune.toml.serialization.TomlField
import dev.openrune.definition.Definition
import dev.openrune.definition.EntityOpsDefinition
import dev.openrune.definition.Parameterized
import dev.openrune.definition.Recolourable
import dev.openrune.definition.Transforms
import dev.openrune.seralizer.NpcTypeOptionsTableHook
import dev.openrune.seralizer.ParamSerializer

/**
 * A loaded npc definition. Immutable apart from [id] (which the load machinery assigns):
 * decoding and packing build one through [NpcTypeBuilder], and everything after that only
 * reads. [actions] is a mutable holder by necessity — the TOML options hook fills it in after
 * construction — but by convention it is not modified once a definition has been built.
 */
@RsTableHeaders(
    "npc",
    rowPostDecode = NpcTypeOptionsTableHook::class,
)
data class NpcType(
    override var id: Int = -1,
    val name: String = "null",
    val size : Int = 1,
    val category : Int = -1,
    val models: List<Int>? = null,
    val chatheadModels: List<Int>? = null,
    val standAnim : Int = -1,
    val rotateLeftAnim : Int = -1,
    val rotateRightAnim : Int = -1,
    val walkAnim : Int = -1,
    val rotateBackAnim : Int = -1,
    val walkLeftAnim : Int = -1,
    val walkRightAnim : Int = -1,
    // The one non-val: the TOML options hook runs after construction and replaces this with the
    // parsed op set. The instance itself is immutable, so sharing stays safe.
    var actions : EntityOpsDefinition = EntityOpsDefinition.EMPTY,
    override val originalColours: List<Int>? = null,
    override val modifiedColours: List<Int>? = null,
    override val originalTextureColours: List<Int>? = null,
    override val modifiedTextureColours: List<Int>? = null,
    override val multiVarBit: Int = -1,
    override val multiVarp: Int = -1,
    override val multiDefault: Int = -1,
    override val transforms: List<Int>? = null,
    val isMinimapVisible : Boolean = true,
    val combatLevel : Int = -1,
    val widthScale : Int = 128,
    val heightScale : Int = 128,
    val renderPriority : Int = 0,
    val ambient : Int = 0,
    val contrast : Int = 0,
    val headIconGraphics: List<Int>? = null,
    val headIconIndexes: List<Int>? = null,
    val rotation : Int = 32,
    val isInteractable : Boolean = true,
    val isClickable : Boolean = true,
    val lowPriorityFollowerOps : Boolean = false,
    val isFollower : Boolean = false,
    val runSequence : Int = -1,
    val runBackSequence : Int = -1,
    val runRightSequence : Int = -1,
    val runLeftSequence : Int = -1,
    val crawlSequence : Int = -1,
    val crawlBackSequence : Int = -1,
    val crawlRightSequence : Int = -1,
    val crawlLeftSequence : Int = -1,
    // Stays MutableMap: ParamSerializer's declared type argument must match the parameter type.
    @param:TomlField(serializer = ParamSerializer::class)
    override val params: MutableMap<Int, Any>? = null,
    val height: Int = -1,
    val attack : Int = 1,
    val defence : Int = 1,
    val strength : Int = 1,
    val hitpoints : Int = 1,
    val ranged : Int = 1,
    val magic : Int = 1,
    val footprintSize : Int = -1,
    val canHideForOverlap : Boolean = false,
    val overlapTintHSL : Int = 39188,
    val readyAnimDuringAnim : Boolean = false,
    val zbuf : Boolean = true,
    val bgSound: BgSound? = null,
    val bgSoundFade: BgSoundFade? = null,
    val crossWorldSound: Int = 2,
    val randomSound: RandomSound? = null,
    ) : Definition, Transforms, Recolourable, Parameterized {

    var examine : String = ""

    // In the body so it stays out of equals/hashCode/toString/copy. Used by the r718 and rs3 codecs.
    private var extraProperties: MutableMap<String, Any?>? = null

    override val extra: MutableMap<String, Any?>
        get() = extraProperties ?: LinkedHashMap<String, Any?>(8).also { extraProperties = it }

    /** A mutable copy of this definition, for tools that need to edit and re-pack it. */
    fun toBuilder(): NpcTypeBuilder = NpcTypeBuilder.from(this)

    fun isAttackable(): Boolean = combatLevel > 0 && actions.opsOrEmpty.any { it?.text == "Attack" }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as NpcType

        if (id != other.id) return false
        if (name != other.name) return false
        if (size != other.size) return false
        if (category != other.category) return false
        if (models != other.models) return false
        if (chatheadModels != other.chatheadModels) return false
        if (standAnim != other.standAnim) return false
        if (rotateLeftAnim != other.rotateLeftAnim) return false
        if (rotateRightAnim != other.rotateRightAnim) return false
        if (walkAnim != other.walkAnim) return false
        if (rotateBackAnim != other.rotateBackAnim) return false
        if (walkLeftAnim != other.walkLeftAnim) return false
        if (walkRightAnim != other.walkRightAnim) return false
        if (actions != other.actions) return false
        if (originalColours != other.originalColours) return false
        if (modifiedColours != other.modifiedColours) return false
        if (originalTextureColours != other.originalTextureColours) return false
        if (modifiedTextureColours != other.modifiedTextureColours) return false
        if (multiVarBit != other.multiVarBit) return false
        if (multiVarp != other.multiVarp) return false
        if (multiDefault != other.multiDefault) return false
        if (transforms != other.transforms) return false
        if (isMinimapVisible != other.isMinimapVisible) return false
        if (combatLevel != other.combatLevel) return false
        if (widthScale != other.widthScale) return false
        if (heightScale != other.heightScale) return false
        if (renderPriority != other.renderPriority) return false
        if (ambient != other.ambient) return false
        if (contrast != other.contrast) return false
        if (headIconGraphics != other.headIconGraphics) return false
        if (headIconIndexes != other.headIconIndexes) return false
        if (rotation != other.rotation) return false
        if (isInteractable != other.isInteractable) return false
        if (isClickable != other.isClickable) return false
        if (lowPriorityFollowerOps != other.lowPriorityFollowerOps) return false
        if (isFollower != other.isFollower) return false
        if (runSequence != other.runSequence) return false
        if (runBackSequence != other.runBackSequence) return false
        if (runRightSequence != other.runRightSequence) return false
        if (runLeftSequence != other.runLeftSequence) return false
        if (crawlSequence != other.crawlSequence) return false
        if (crawlBackSequence != other.crawlBackSequence) return false
        if (crawlRightSequence != other.crawlRightSequence) return false
        if (crawlLeftSequence != other.crawlLeftSequence) return false
        if (params != other.params) return false
        if (height != other.height) return false
        if (attack != other.attack) return false
        if (defence != other.defence) return false
        if (strength != other.strength) return false
        if (hitpoints != other.hitpoints) return false
        if (ranged != other.ranged) return false
        if (magic != other.magic) return false
        if (examine != other.examine) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + name.hashCode()
        result = 31 * result + size
        result = 31 * result + category
        result = 31 * result + (models?.hashCode() ?: 0)
        result = 31 * result + (chatheadModels?.hashCode() ?: 0)
        result = 31 * result + standAnim
        result = 31 * result + rotateLeftAnim
        result = 31 * result + rotateRightAnim
        result = 31 * result + walkAnim
        result = 31 * result + rotateBackAnim
        result = 31 * result + walkLeftAnim
        result = 31 * result + walkRightAnim
        result = 31 * result + actions.hashCode()
        result = 31 * result + (originalColours?.hashCode() ?: 0)
        result = 31 * result + (modifiedColours?.hashCode() ?: 0)
        result = 31 * result + (originalTextureColours?.hashCode() ?: 0)
        result = 31 * result + (modifiedTextureColours?.hashCode() ?: 0)
        result = 31 * result + multiVarBit
        result = 31 * result + multiVarp
        result = 31 * result + multiDefault
        result = 31 * result + (transforms?.hashCode() ?: 0)
        result = 31 * result + isMinimapVisible.hashCode()
        result = 31 * result + combatLevel
        result = 31 * result + widthScale
        result = 31 * result + heightScale
        result = 31 * result + renderPriority.hashCode()
        result = 31 * result + ambient
        result = 31 * result + contrast
        result = 31 * result + (headIconGraphics?.hashCode() ?: 0)
        result = 31 * result + (headIconIndexes?.hashCode() ?: 0)
        result = 31 * result + rotation
        result = 31 * result + isInteractable.hashCode()
        result = 31 * result + isClickable.hashCode()
        result = 31 * result + lowPriorityFollowerOps.hashCode()
        result = 31 * result + isFollower.hashCode()
        result = 31 * result + runSequence
        result = 31 * result + runBackSequence
        result = 31 * result + runRightSequence
        result = 31 * result + runLeftSequence
        result = 31 * result + crawlSequence
        result = 31 * result + crawlBackSequence
        result = 31 * result + crawlRightSequence
        result = 31 * result + crawlLeftSequence
        result = 31 * result + (params?.hashCode() ?: 0)
        result = 31 * result + height
        result = 31 * result + attack
        result = 31 * result + defence
        result = 31 * result + strength
        result = 31 * result + hitpoints
        result = 31 * result + ranged
        result = 31 * result + magic
        result = 31 * result + examine.hashCode()

        return result
    }
}
