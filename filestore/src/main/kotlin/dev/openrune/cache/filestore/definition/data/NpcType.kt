package dev.openrune.cache.filestore.definition.data

import dev.openrune.cache.filestore.definition.Definition
import dev.openrune.cache.filestore.definition.Parameterized
import dev.openrune.cache.filestore.definition.Recolourable
import dev.openrune.cache.filestore.definition.Transforms
import dev.openrune.serialization.ListRscm
import dev.openrune.serialization.Rscm
import dev.openrune.serialization.RscmList
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

data class NpcType(
    override var id: Rscm = -1,
    override val values: MutableMap<String, Any> = Object2ObjectOpenHashMap(),
    //Custom
    override var inherit: Int = -1,
    ) : Definition {

    init {
        values["actions"] = mutableListOf(null,null,null,null,null)
        values["stats"] = intArrayOf(1, 1, 1, 1, 1, 1)
    }

    var examine: String
        get() = values.getOrDefault("examine", "") as String
        set(value) { values["examine"] = value }


    fun isAttackable(): Boolean = getCombatLevel() > 0 && getActions().any { it == "Attack" }

    fun getName(): String = values.getOrDefault("name", "null") as String
    fun getSize(): Int = values.getOrDefault("size", 1) as Int
    fun getCategory(): Int = values.getOrDefault("category", -1) as Int
    fun getModels(): MutableList<Int>? = values["models"] as? MutableList<Int>
    fun getChatheadModels(): MutableList<Int>? = values["chatheadModels"] as? MutableList<Int>

    fun getStandAnim(): Int = values.getOrDefault("standAnim", -1) as Int
    fun getRotateLeftAnim(): Int = values.getOrDefault("rotateLeftAnim", -1) as Int
    fun getRotateRightAnim(): Int = values.getOrDefault("rotateRightAnim", -1) as Int
    fun getWalkAnim(): Int = values.getOrDefault("walkAnim", -1) as Int
    fun getRotateBackAnim(): Int = values.getOrDefault("rotateBackAnim", -1) as Int
    fun getWalkLeftAnim(): Int = values.getOrDefault("walkLeftAnim", -1) as Int
    fun getWalkRightAnim(): Int = values.getOrDefault("walkRightAnim", -1) as Int
    fun getActions(): MutableList<String?> = values["actions"] as MutableList<String?>

    fun getOriginalColours(): MutableList<Int>? = values["originalColours"] as? MutableList<Int>
    fun getModifiedColours(): MutableList<Int>? = values["modifiedColours"] as? MutableList<Int>
    fun getOriginalTextureColours(): MutableList<Int>? = values["originalTextureColours"] as? MutableList<Int>
    fun getModifiedTextureColours(): MutableList<Int>? = values["modifiedTextureColours"] as? MutableList<Int>
    fun getVarbit(): Int = values.getOrDefault("varbit", -1) as Int
    fun getVarp(): Int = values.getOrDefault("varp", -1) as Int
    fun getTransforms(): MutableList<Int>? = values["transforms"] as? MutableList<Int>

    fun isMinimapVisible(): Boolean = values.getOrDefault("isMinimapVisible", true) as Boolean
    fun getCombatLevel(): Int = values.getOrDefault("combatLevel", -1) as Int
    fun getWidthScale(): Int = values.getOrDefault("widthScale", 128) as Int
    fun getHeightScale(): Int = values.getOrDefault("heightScale", 128) as Int
    fun hasRenderPriority(): Boolean = values.getOrDefault("hasRenderPriority", false) as Boolean
    fun getAmbient(): Int = values.getOrDefault("ambient", 0) as Int
    fun getContrast(): Int = values.getOrDefault("contrast", 0) as Int
    fun getHeadIconArchiveIds(): MutableList<Int>? = values["headIconArchiveIds"] as? MutableList<Int>
    fun getHeadIconSpriteIndex(): MutableList<Int>? = values["headIconSpriteIndex"] as? MutableList<Int>
    fun getRotation(): Int = values.getOrDefault("rotation", 32) as Int
    fun isInteractable(): Boolean = values.getOrDefault("isInteractable", true) as Boolean
    fun isClickable(): Boolean = values.getOrDefault("isClickable", true) as Boolean
    fun hasLowPriorityFollowerOps(): Boolean = values.getOrDefault("lowPriorityFollowerOps", false) as Boolean
    fun isFollower(): Boolean = values.getOrDefault("isFollower", false) as Boolean

    fun getRunSequence(): Int = values.getOrDefault("runSequence", -1) as Int
    fun getRunBackSequence(): Int = values.getOrDefault("runBackSequence", -1) as Int
    fun getRunRightSequence(): Int = values.getOrDefault("runRightSequence", -1) as Int
    fun getRunLeftSequence(): Int = values.getOrDefault("runLeftSequence", -1) as Int
    fun getCrawlSequence(): Int = values.getOrDefault("crawlSequence", -1) as Int
    fun getCrawlBackSequence(): Int = values.getOrDefault("crawlBackSequence", -1) as Int
    fun getCrawlRightSequence(): Int = values.getOrDefault("crawlRightSequence", -1) as Int
    fun getCrawlLeftSequence(): Int = values.getOrDefault("crawlLeftSequence", -1) as Int
    fun getParams(): Map<Int, Any>? = values["params"] as? Map<Int, Any>

    fun getHeight(): Int = values.getOrDefault("height", -1) as Int
    fun getStats(): IntArray = values.getOrDefault("stats", intArrayOf(1, 1, 1, 1, 1, 1)) as IntArray

    companion object {
        const val ATTACK = 0
        const val DEFENCE = 1
        const val STRENGTH = 2
        const val HITPOINTS = 3
        const val RANGED = 4
        const val MAGIC = 5
    }
}