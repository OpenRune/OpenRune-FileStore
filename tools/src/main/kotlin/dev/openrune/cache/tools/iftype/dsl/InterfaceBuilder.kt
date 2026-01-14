package dev.openrune.cache.tools.iftype.dsl

import dev.openrune.cache.filestore.definition.InterfaceType
import dev.openrune.definition.type.widget.ComponentType
import dev.openrune.definition.type.widget.ComponentTypeBuilder

/**
 * Copies all properties from source to target ComponentTypeBuilder
 */
private fun ComponentTypeBuilder.copyFrom(source: ComponentTypeBuilder) {
    v3 = source.v3
    type = source.type
    buttonType = source.buttonType
    contentType = source.contentType
    width = source.width
    height = source.height
    trans1 = source.trans1
    layer = source.layer
    mouseOverRedirect = source.mouseOverRedirect
    cs1Comparisons = source.cs1Comparisons
    cs1ComparisonValues = source.cs1ComparisonValues
    cs1Instructions = source.cs1Instructions
    scrollHeight = source.scrollHeight
    hide = source.hide
    fill = source.fill
    textAlignH = source.textAlignH
    textAlignV = source.textAlignV
    textLineHeight = source.textLineHeight
    textFont = source.textFont
    textShadow = source.textShadow
    secondaryText = source.secondaryText
    colour1 = source.colour1
    colour2 = source.colour2
    mouseOverColour1 = source.mouseOverColour1
    mouseOverColour2 = source.mouseOverColour2
    graphic = source.graphic
    secondaryGraphic = source.secondaryGraphic
    modelKind = source.modelKind
    model = source.model
    secondaryModelKind = source.secondaryModelKind
    secondaryModel = source.secondaryModel
    modelAnim = source.modelAnim
    secondaryModelAnim = source.secondaryModelAnim
    modelZoom = source.modelZoom
    modelAngleX = source.modelAngleX
    modelAngleY = source.modelAngleY
    text = source.text
    targetVerb = source.targetVerb
    targetBase = source.targetBase
    events = source.events
    buttonText = source.buttonText
    widthMode = source.widthMode
    heightMode = source.heightMode
    xMode = source.xMode
    yMode = source.yMode
    scrollWidth = source.scrollWidth
    noClickThrough = source.noClickThrough
    angle2d = source.angle2d
    tiling = source.tiling
    outline = source.outline
    graphicShadow = source.graphicShadow
    vFlip = source.vFlip
    hFlip = source.hFlip
    modelX = source.modelX
    modelY = source.modelY
    modelAngleZ = source.modelAngleZ
    modelOrthog = source.modelOrthog
    modelObjWidth = source.modelObjWidth
    lineWid = source.lineWid
    lineDirection = source.lineDirection
    opBase = source.opBase
    op = source.op
    dragDeadZone = source.dragDeadZone
    dragDeadTime = source.dragDeadTime
    draggableBehavior = source.draggableBehavior
    onLoad = source.onLoad
    onMouseOver = source.onMouseOver
    onMouseLeave = source.onMouseLeave
    onTargetLeave = source.onTargetLeave
    onTargetEnter = source.onTargetEnter
    onVarTransmit = source.onVarTransmit
    onInvTransmit = source.onInvTransmit
    onStatTransmit = source.onStatTransmit
    onTimer = source.onTimer
    onOp = source.onOp
    onMouseRepeat = source.onMouseRepeat
    onClick = source.onClick
    onClickRepeat = source.onClickRepeat
    onRelease = source.onRelease
    onHold = source.onHold
    onDrag = source.onDrag
    onDragComplete = source.onDragComplete
    onScrollWheel = source.onScrollWheel
    onVarTransmitList = source.onVarTransmitList
    onInvTransmitList = source.onInvTransmitList
    onStatTransmitList = source.onStatTransmitList
}

sealed class RepeatType {
    open fun generateComponents(widthCom : Int, heightCom : Int, namePrefix: String, component: ComponentTypeBuilder): List<ComponentTypeBuilder> = emptyList()
}

data class Grid(
    val count: Int,
    val rowSize: Int,
    val padX: Int = 0,
    val padY: Int = 0,
    val additive: Boolean = true
) : RepeatType() {
    override fun generateComponents(
        widthCom: Int,
        heightCom: Int,
        namePrefix: String,
        component: ComponentTypeBuilder
    ): List<ComponentTypeBuilder> {

        val baseX = component.x ?: 0
        val baseY = component.y ?: 0

        return List(count) { index ->
            val row = index / rowSize
            val col = index % rowSize

            ComponentTypeBuilder("${namePrefix}_$index").apply {
                copyFrom(component)
                x = baseX + col * padX
                y = baseY + row * padY
            }
        }
    }

}

class InterfaceBuilder(
    var id: Int = -1,
    var width: Int = -1,
    var height: Int = -1
) {
    var onLoad: Array<Any>? = null

    fun apply(componentName : String): ComponentTypeBuilder {
        return ComponentTypeBuilder(componentName).apply {
            this.width = this@InterfaceBuilder.width
            this.height = this@InterfaceBuilder.height
            v3 = true
            onLoad = this@InterfaceBuilder.onLoad
        }
    }

    var offset = Pair(0,0)
    val components = mutableListOf<ComponentTypeBuilder>()

    fun setOffset(block: () -> Pair<Int, Int>) {
        val (newX, newY) = block()
        offset = Pair(newX,newY)
    }

    fun onLoadListener(block: () -> Array<Any>?) {
        onLoad = block()
    }

    private fun totalChildren() = components.size
}

fun buildInterface(id: Int, interfaceName: String, width: Int, height: Int,builder: InterfaceBuilder.() -> Unit): InterfaceType {
    val bld = InterfaceBuilder(id,width,height)
    builder.invoke(bld)

    val component = bld.apply("universe")

    bld.components.add(0,component)

    val componentsMap = bld.components.mapIndexed { index, builder ->
        builder.x = (builder.x ?: 0) + bld.offset.first
        builder.y = (builder.y ?: 0) + bld.offset.second
        builder.v3 = true
        index to builder.build((id shl 16) or index)
    }.toMap()
    
    return InterfaceType(
        components = componentsMap,
        _internalId = id,
        _internalName = interfaceName
    )
}
