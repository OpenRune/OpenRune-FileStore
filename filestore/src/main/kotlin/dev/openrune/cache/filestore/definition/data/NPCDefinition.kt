package dev.openrune.cache.filestore.definition.data

import dev.openrune.cache.filestore.definition.Definition
import dev.openrune.cache.filestore.definition.Parameterized
import dev.openrune.cache.filestore.definition.Recolourable
import dev.openrune.cache.filestore.definition.Transforms

data class NPCDefinition(
    override var id: Int = -1,
    var name: String = "null",
    var size : Int = 1,
    var category : Int = -1,
    var models: MutableList<Int>? = null,
    var chatheadModels: MutableList<Int>? = null,
    var standAnim : Int = -1,
    var rotateLeftAnim : Int = -1,
    var rotateRightAnim : Int = -1,
    var walkAnim : Int = -1,
    var rotateBackAnim : Int = -1,
    var walkLeftAnim : Int = -1,
    var walkRightAnim : Int = -1,
    var actions : MutableList<String?> = mutableListOf(null, null, null, null, null),
    override var originalColours: MutableList<Short>? = null,
    override var modifiedColours: MutableList<Short>? = null,
    override var originalTextureColours: MutableList<Short>? = null,
    override var modifiedTextureColours: MutableList<Short>? = null,
    override var varbit: Int = -1,
    override var varp: Int = -1,
    override var transforms: MutableList<Int>? = null,
    var isMinimapVisible : Boolean = true,
    var combatLevel : Int = -1,
    var widthScale : Int = 128,
    var heightScale : Int = 128,
    var hasRenderPriority : Boolean = false,
    var ambient : Int = 0,
    var contrast : Int = 0,
    var headIconArchiveIds: MutableList<Int>? = null,
    var headIconSpriteIndex: MutableList<Int>? = null,
    var rotation : Int = 32,
    var isInteractable : Boolean = true,
    var isClickable : Boolean = true,
    var lowPriorityFollowerOps : Boolean = false,
    var isFollower : Boolean = false,
    var runSequence : Int = -1,
    var runBackSequence : Int = -1,
    var runRightSequence : Int = -1,
    var runLeftSequence : Int = -1,
    var crawlSequence : Int = -1,
    var crawlBackSequence : Int = -1,
    var crawlRightSequence : Int = -1,
    var crawlLeftSequence : Int = -1,
    override var params: Map<Int, Any>? = null,
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

    fun isAttackable(): Boolean = combatLevel > 0 && actions.any { it == "Attack" }

}