package dev.openrune.definition.type.builders

import dev.openrune.definition.type.*

import dev.openrune.definition.EntityOpsDefinition
import dev.openrune.definition.MutableParameterized
import dev.openrune.definition.MutableRecolourable
import dev.openrune.definition.MutableTransforms
import dev.openrune.definition.util.IntListPool

class NpcTypeBuilder(var id: Int = -1) : MutableTransforms, MutableRecolourable, MutableParameterized {

    var name: String = "null"
    var size: Int = 1
    var category: Int = -1
    var models: MutableList<Int>? = null
    var chatheadModels: MutableList<Int>? = null
    var standAnim: Int = -1
    var rotateLeftAnim: Int = -1
    var rotateRightAnim: Int = -1
    var walkAnim: Int = -1
    var rotateBackAnim: Int = -1
    var walkLeftAnim: Int = -1
    var walkRightAnim: Int = -1
    var actions: EntityOpsDefinition = EntityOpsDefinition.EMPTY
    var isMinimapVisible: Boolean = true
    var combatLevel: Int = -1
    var widthScale: Int = 128
    var heightScale: Int = 128
    var renderPriority: Int = 0
    var ambient: Int = 0
    var contrast: Int = 0
    var headIconGraphics: MutableList<Int>? = null
    var headIconIndexes: MutableList<Int>? = null
    var rotation: Int = 32
    var isInteractable: Boolean = true
    var isClickable: Boolean = true
    var lowPriorityFollowerOps: Boolean = false
    var isFollower: Boolean = false
    var runSequence: Int = -1
    var runBackSequence: Int = -1
    var runRightSequence: Int = -1
    var runLeftSequence: Int = -1
    var crawlSequence: Int = -1
    var crawlBackSequence: Int = -1
    var crawlRightSequence: Int = -1
    var crawlLeftSequence: Int = -1
    var height: Int = -1
    var attack: Int = 1
    var defence: Int = 1
    var strength: Int = 1
    var hitpoints: Int = 1
    var ranged: Int = 1
    var magic: Int = 1
    var footprintSize: Int = -1
    var canHideForOverlap: Boolean = false
    var overlapTintHSL: Int = 39188
    var readyAnimDuringAnim: Boolean = false
    var zbuf: Boolean = true
    var bgSound: BgSound? = null
    var bgSoundFade: BgSoundFade? = null
    var crossWorldSound: Int = 2
    var randomSound: RandomSound? = null
    var examine: String = ""

    override var originalColours: MutableList<Int>? = null
    override var modifiedColours: MutableList<Int>? = null
    override var originalTextureColours: MutableList<Int>? = null
    override var modifiedTextureColours: MutableList<Int>? = null
    override var multiVarBit: Int = -1
    override var multiVarp: Int = -1
    override var multiDefault: Int = -1
    override var transforms: MutableList<Int>? = null
    override var params: MutableMap<Int, Any>? = null

    /** Loose, codec-specific properties (r718/rs3), applied to [NpcType.extra] on [build]. */
    private val extras = LinkedHashMap<String, Any?>()

    fun setExtraProperty(key: String, value: Any?) {
        extras[key] = value
    }

    fun build(): NpcType {
        val built = NpcType(
            id = id,
            name = name,
            size = size,
            category = category,
            models = IntListPool.of(models),
            chatheadModels = IntListPool.of(chatheadModels),
            standAnim = standAnim,
            rotateLeftAnim = rotateLeftAnim,
            rotateRightAnim = rotateRightAnim,
            walkAnim = walkAnim,
            rotateBackAnim = rotateBackAnim,
            walkLeftAnim = walkLeftAnim,
            walkRightAnim = walkRightAnim,
            actions = actions,
            originalColours = IntListPool.of(originalColours),
            modifiedColours = IntListPool.of(modifiedColours),
            originalTextureColours = IntListPool.of(originalTextureColours),
            modifiedTextureColours = IntListPool.of(modifiedTextureColours),
            multiVarBit = multiVarBit,
            multiVarp = multiVarp,
            multiDefault = multiDefault,
            transforms = IntListPool.of(transforms),
            isMinimapVisible = isMinimapVisible,
            combatLevel = combatLevel,
            widthScale = widthScale,
            heightScale = heightScale,
            renderPriority = renderPriority,
            ambient = ambient,
            contrast = contrast,
            headIconGraphics = IntListPool.of(headIconGraphics),
            headIconIndexes = IntListPool.of(headIconIndexes),
            rotation = rotation,
            isInteractable = isInteractable,
            isClickable = isClickable,
            lowPriorityFollowerOps = lowPriorityFollowerOps,
            isFollower = isFollower,
            runSequence = runSequence,
            runBackSequence = runBackSequence,
            runRightSequence = runRightSequence,
            runLeftSequence = runLeftSequence,
            crawlSequence = crawlSequence,
            crawlBackSequence = crawlBackSequence,
            crawlRightSequence = crawlRightSequence,
            crawlLeftSequence = crawlLeftSequence,
            params = params,
            height = height,
            attack = attack,
            defence = defence,
            strength = strength,
            hitpoints = hitpoints,
            ranged = ranged,
            magic = magic,
            footprintSize = footprintSize,
            canHideForOverlap = canHideForOverlap,
            overlapTintHSL = overlapTintHSL,
            readyAnimDuringAnim = readyAnimDuringAnim,
            zbuf = zbuf,
            bgSound = bgSound,
            bgSoundFade = bgSoundFade,
            crossWorldSound = crossWorldSound,
            randomSound = randomSound,
        )
        built.examine = examine
        if (extras.isNotEmpty()) built.extra.putAll(extras)
        return built
    }

    companion object {
        /** Copies every field of [type] into a fresh builder. List contents are copied too. */
        fun from(type: NpcType): NpcTypeBuilder {
            val builder = NpcTypeBuilder(type.id)
            builder.name = type.name
            builder.size = type.size
            builder.category = type.category
            builder.models = type.models?.toMutableList()
            builder.chatheadModels = type.chatheadModels?.toMutableList()
            builder.standAnim = type.standAnim
            builder.rotateLeftAnim = type.rotateLeftAnim
            builder.rotateRightAnim = type.rotateRightAnim
            builder.walkAnim = type.walkAnim
            builder.rotateBackAnim = type.rotateBackAnim
            builder.walkLeftAnim = type.walkLeftAnim
            builder.walkRightAnim = type.walkRightAnim
            builder.actions = type.actions
            builder.originalColours = type.originalColours?.toMutableList()
            builder.modifiedColours = type.modifiedColours?.toMutableList()
            builder.originalTextureColours = type.originalTextureColours?.toMutableList()
            builder.modifiedTextureColours = type.modifiedTextureColours?.toMutableList()
            builder.multiVarBit = type.multiVarBit
            builder.multiVarp = type.multiVarp
            builder.multiDefault = type.multiDefault
            builder.transforms = type.transforms?.toMutableList()
            builder.isMinimapVisible = type.isMinimapVisible
            builder.combatLevel = type.combatLevel
            builder.widthScale = type.widthScale
            builder.heightScale = type.heightScale
            builder.renderPriority = type.renderPriority
            builder.ambient = type.ambient
            builder.contrast = type.contrast
            builder.headIconGraphics = type.headIconGraphics?.toMutableList()
            builder.headIconIndexes = type.headIconIndexes?.toMutableList()
            builder.rotation = type.rotation
            builder.isInteractable = type.isInteractable
            builder.isClickable = type.isClickable
            builder.lowPriorityFollowerOps = type.lowPriorityFollowerOps
            builder.isFollower = type.isFollower
            builder.runSequence = type.runSequence
            builder.runBackSequence = type.runBackSequence
            builder.runRightSequence = type.runRightSequence
            builder.runLeftSequence = type.runLeftSequence
            builder.crawlSequence = type.crawlSequence
            builder.crawlBackSequence = type.crawlBackSequence
            builder.crawlRightSequence = type.crawlRightSequence
            builder.crawlLeftSequence = type.crawlLeftSequence
            builder.params = type.params?.toMutableMap()
            builder.height = type.height
            builder.attack = type.attack
            builder.defence = type.defence
            builder.strength = type.strength
            builder.hitpoints = type.hitpoints
            builder.ranged = type.ranged
            builder.magic = type.magic
            builder.footprintSize = type.footprintSize
            builder.canHideForOverlap = type.canHideForOverlap
            builder.overlapTintHSL = type.overlapTintHSL
            builder.readyAnimDuringAnim = type.readyAnimDuringAnim
            builder.zbuf = type.zbuf
            builder.bgSound = type.bgSound
            builder.bgSoundFade = type.bgSoundFade
            builder.crossWorldSound = type.crossWorldSound
            builder.randomSound = type.randomSound
            builder.examine = type.examine
            // Extras are intentionally not copied: NpcType.extra allocates its backing map on
            // first read, so probing it here would mutate the source. Codec-specific extras do
            // not survive toBuilder().
            return builder
        }
    }
}
