package dev.openrune.cache.filestore.definition.data

import dev.openrune.cache.filestore.definition.Definition
import dev.openrune.cache.filestore.definition.Parameterized
import dev.openrune.cache.filestore.definition.Recolourable
import dev.openrune.cache.filestore.definition.Transforms
import dev.openrune.cache.filestore.serialization.UShortList
import dev.openrune.cache.filestore.serialization.toIntWithMaxCheck
import kotlinx.serialization.Polymorphic


data class NpcType(
    override var id: Int = -1,
    var name: String = "null",
    var size : UByte = 1u,
    var category : UShort = UShort.MAX_VALUE,
    var models: UShortList = emptyList(),
    var chatheadModels: UShortList = emptyList(),
    var standAnim : UShort = UShort.MAX_VALUE,
    var rotateLeftAnim : UShort = UShort.MAX_VALUE,
    var rotateRightAnim : UShort = UShort.MAX_VALUE,
    var walkAnim : UShort = UShort.MAX_VALUE,
    var rotateBackAnim : UShort = UShort.MAX_VALUE,
    var walkLeftAnim : UShort = UShort.MAX_VALUE,
    var walkRightAnim : UShort = UShort.MAX_VALUE,
    var actions : MutableList<String?> = mutableListOf(null, null, null, null, null),
    override var originalColours: UShortList = emptyList(),
    override var modifiedColours: UShortList = emptyList(),
    override var originalTextureColours: UShortList = emptyList(),
    override var modifiedTextureColours: UShortList = emptyList(),
    override var varbit: Int = -1,
    override var varp: Int = -1,
    override var transforms: MutableList<Int>? = null,
    var isMinimapVisible : Boolean = true,
    var combatLevel : UShort = UShort.MAX_VALUE,
    var widthScale : UShort = 128u,
    var heightScale : UShort = 128u,
    var hasRenderPriority : Boolean = false,
    var ambient : Byte = 0,
    var contrast : Byte = 0,
    var headIconArchiveIds: MutableList<Int>? = null,
    var headIconSpriteIndex: MutableList<Int>? = null,
    var rotation : UShort = 32u,
    var isInteractable : Boolean = true,
    var isClickable : Boolean = true,
    var lowPriorityFollowerOps : Boolean = false,
    var isFollower : Boolean = false,
    var runSequence : UShort = UShort.MAX_VALUE,
    var runBackSequence : UShort = UShort.MAX_VALUE,
    var runRightSequence : UShort = UShort.MAX_VALUE,
    var runLeftSequence : UShort = UShort.MAX_VALUE,
    var crawlSequence : UShort = UShort.MAX_VALUE,
    var crawlBackSequence : UShort = UShort.MAX_VALUE,
    var crawlRightSequence : UShort = UShort.MAX_VALUE,
    var crawlLeftSequence : UShort = UShort.MAX_VALUE,
    override var params: Map<Int, @Polymorphic DataValue?>? = null,
    var gfx2dHeight: UShort = UShort.MAX_VALUE,
    var stats: UShortArray = ushortArrayOf(1u, 1u, 1u, 1u, 1u, 1u),

    //Custom
    override var inherit: Int = -1,
    var option1: String? = null,
    var option2: String? = null,
    var option3: String? = null,
    var option4: String? = null,
    var option5: String? = null,

    ) : Definition, Transforms, Recolourable, Parameterized {

    init {
        actions = listOf(option1,option2,option3,option4,option5).toMutableList()
    }

    var examine : String = ""

    fun isAttackable(): Boolean = getCombatLevel() > 0 && actions.any { it == "Attack" }

    companion object {
        const val ATTACK = 0
        const val DEFENCE = 1
        const val STRENGTH = 2
        const val HITPOINTS = 3
        const val RANGED = 4
        const val MAGIC = 5
    }

    fun getCategory(): Int = category.toIntWithMaxCheck()
    fun getStandAnim(): Int = standAnim.toIntWithMaxCheck()
    fun getRotateLeftAnim(): Int = rotateLeftAnim.toIntWithMaxCheck()
    fun getRotateRightAnim(): Int = rotateRightAnim.toIntWithMaxCheck()
    fun getWalkAnim(): Int = walkAnim.toIntWithMaxCheck()
    fun getRotateBackAnim(): Int = rotateBackAnim.toIntWithMaxCheck()
    fun getWalkLeftAnim(): Int = walkLeftAnim.toIntWithMaxCheck()
    fun getWalkRightAnim(): Int = walkRightAnim.toIntWithMaxCheck()
    fun getCombatLevel(): Int = combatLevel.toIntWithMaxCheck()
    fun getWidthScale(): Int = widthScale.toIntWithMaxCheck()
    fun getHeightScale(): Int = heightScale.toIntWithMaxCheck()
    fun getRotation(): Int = rotation.toIntWithMaxCheck()
    fun getRunSequence(): Int = runSequence.toIntWithMaxCheck()
    fun getRunBackSequence(): Int = runBackSequence.toIntWithMaxCheck()
    fun getRunRightSequence(): Int = runRightSequence.toIntWithMaxCheck()
    fun getRunLeftSequence(): Int = runLeftSequence.toIntWithMaxCheck()
    fun getCrawlSequence(): Int = crawlSequence.toIntWithMaxCheck()
    fun getCrawlBackSequence(): Int = crawlBackSequence.toIntWithMaxCheck()
    fun getCrawlRightSequence(): Int = crawlRightSequence.toIntWithMaxCheck()
    fun getCrawlLeftSequence(): Int = crawlLeftSequence.toIntWithMaxCheck()
    fun getHeight(): Int = gfx2dHeight.toIntWithMaxCheck()

    // Getters for UByte properties returning Int
    fun getSize(): Int = size.toIntWithMaxCheck()

    // Getters for Byte properties returning Int
    fun getAmbient(): Int = ambient.toInt()
    fun getContrast(): Int = contrast.toInt()

}