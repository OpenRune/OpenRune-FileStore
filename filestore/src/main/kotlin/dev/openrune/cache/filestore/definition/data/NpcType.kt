package dev.openrune.cache.filestore.definition.data

import dev.openrune.*
import dev.openrune.cache.filestore.definition.Definition
import dev.openrune.serialization.Rscm
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap

data class NpcType(
    override var id: Rscm = -1,
    override val values: MutableMap<String, Any> = Object2ObjectOpenHashMap(),
) : Definition {

    init {
        values["actions"] = listOf(null, null, null, null, null)
        values["stats"] = intArrayOf(1, 1, 1, 1, 1, 1)
    }

    var examine: String
        get() = values.getAsString("examine", "")
        set(value) { values["examine"] = value }

    fun isAttackable() = getCombatLevel() > 0 && getActions().any { it == "Attack" }

    fun getName() = values.getAsString("name")
    fun getSize() = values.getAsInt("size", 1)
    fun getCategory() = values.getAsInt("category", -1)
    fun getModels() = values.getAsListInt("models")
    fun getChatheadModels() = values.getAsListInt("chatheadModels")

    fun getStandAnim() = values.getAsInt("standAnim")
    fun getRotateLeftAnim() = values.getAsInt("rotateLeftAnim")
    fun getRotateRightAnim() = values.getAsInt("rotateRightAnim")
    fun getWalkAnim() = values.getAsInt("walkAnim")
    fun getRotateBackAnim() = values.getAsInt("rotateBackAnim")
    fun getWalkLeftAnim() = values.getAsInt("walkLeftAnim")
    fun getWalkRightAnim() = values.getAsInt("walkRightAnim")
    fun getActions() = values.getAsListString("actions") ?: listOf(null, null, null, null, null)

    fun getOriginalColours() = values.getAsListInt("originalColours")
    fun getModifiedColours() = values.getAsListInt("modifiedColours")
    fun getOriginalTextureColours() = values.getAsListInt("originalTextureColours")
    fun getModifiedTextureColours() = values.getAsListInt("modifiedTextureColours")
    fun getVarbit() = values.getAsInt("varbit")
    fun getVarp() = values.getAsInt("varp")
    fun getTransforms() = values.getAsListInt("transforms")

    fun isMinimapVisible() = values.getAsBoolean("isMinimapVisible")
    fun getCombatLevel() = values.getAsInt("combatLevel")
    fun getWidthScale() = values.getAsInt("widthScale", 128)
    fun getHeightScale() = values.getAsInt("heightScale", 128)
    fun hasRenderPriority() = values.getAsBoolean("hasRenderPriority", false)
    fun getAmbient() = values.getAsInt("ambient")
    fun getContrast() = values.getAsInt("contrast")
    fun getHeadIconArchiveIds() = values.getAsListInt("headIconArchiveIds")
    fun getHeadIconSpriteIndex() = values.getAsListInt("headIconSpriteIndex")
    fun getRotation() = values.getAsInt("rotation", 32)
    fun isInteractable() = values.getAsBoolean("isInteractable")
    fun isClickable() = values.getAsBoolean("isClickable")
    fun hasLowPriorityFollowerOps() = values.getAsBoolean("lowPriorityFollowerOps", false)
    fun isFollower() = values.getAsBoolean("isFollower")

    fun getRunSequence() = values.getAsInt("runSequence")
    fun getRunBackSequence() = values.getAsInt("runBackSequence")
    fun getRunRightSequence() = values.getAsInt("runRightSequence")
    fun getRunLeftSequence() = values.getAsInt("runLeftSequence")
    fun getCrawlSequence() = values.getAsInt("crawlSequence")
    fun getCrawlBackSequence() = values.getAsInt("crawlBackSequence")
    fun getCrawlRightSequence() = values.getAsInt("crawlRightSequence")
    fun getCrawlLeftSequence() = values.getAsInt("crawlLeftSequence")
    fun getParams() = values["params"] as? Map<Int, Any>

    fun getHeight() = values.getAsInt("height")
    fun getStats() = values.getOrDefault("stats", intArrayOf(1, 1, 1, 1, 1, 1)) as IntArray

    companion object {
        const val ATTACK = 0
        const val DEFENCE = 1
        const val STRENGTH = 2
        const val HITPOINTS = 3
        const val RANGED = 4
        const val MAGIC = 5
    }

    override fun toString(): String {
        val stringBuilder = StringBuilder()

        // Append the regular properties like `id` and `inherit`
        stringBuilder.append("NpcType(id=$id, values={")

        // Append the values map without map syntax, just the key-value pairs
        if (values.isNotEmpty()) {
            values.forEach { (key, value) ->
                stringBuilder.append("$key=$value, ")
            }
            // Remove the trailing comma and space
            if (stringBuilder.endsWith(", ")) {
                stringBuilder.setLength(stringBuilder.length - 2)
            }
        }

        stringBuilder.append("})")

        return stringBuilder.toString()
    }

}
