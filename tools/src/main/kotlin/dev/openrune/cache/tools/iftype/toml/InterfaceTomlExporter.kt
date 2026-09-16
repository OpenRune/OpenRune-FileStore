package dev.openrune.cache.tools.iftype.toml

import dev.openrune.cache.filestore.definition.InterfaceType
import dev.openrune.definition.constants.ConstantProvider
import dev.openrune.definition.type.widget.Component.Companion.CHILD_BIT_MASK
import dev.openrune.definition.type.widget.Component.Companion.CHILD_BIT_OFFSET
import dev.openrune.definition.type.widget.ComponentType
import dev.openrune.definition.type.widget.IfEvent
import java.nio.file.Path
import kotlin.io.path.writeText

private val EVENT_FLAGS = IfEvent.entries.filter { it.bitmask in 1 until (1L shl 32) && it.bitmask.countOneBits() == 1 }
private val SPRITE_TABLES = arrayOf("sprites", "sprite")
private val MODEL_TABLES = arrayOf("models", "model")
private val ANIMATION_TABLES = arrayOf("seq", "sequences", "animation")
private val TEXTURE_TABLES = arrayOf("textures", "texture")
private val SCRIPT_TABLES = arrayOf("clientscript", "ifscript", "script")
private val COMPONENT_TABLES = arrayOf("component")

private const val MIN_PACKED_COMPONENT_ID = 1 shl 16

typealias HookArgResolver = (scriptId: Int, argIndex: Int, value: Int) -> String?

fun InterfaceType.toToml(hookArgResolver: HookArgResolver? = null): String {
    val root = components[0]
    val names = components.mapValues { (index, comp) -> comp.internalName ?: "com_$index" }

    val sb = StringBuilder()
    sb.append("[[interface]]\n")
    sb.append("name = \"interface.").append(internalName).append("\"\n")
    sb.append("width = ").append(root?.width ?: 0).append('\n')
    sb.append("height = ").append(root?.height ?: 0).append('\n')
    root?.onLoad?.let { hook -> appendHookLine(sb, "on_load", hook, hookArgResolver) }

    for ((index, comp) in components.entries.sortedBy { it.key }) {
        if (index == 0) continue
        sb.append('\n')
        writeComponent(sb, comp, index, names, hookArgResolver)
    }

    return sb.toString()
}

fun InterfaceType.writeToml(path: Path, hookArgResolver: HookArgResolver? = null) =
    path.writeText(toToml(hookArgResolver))

private fun writeComponent(
    sb: StringBuilder,
    comp: ComponentType,
    index: Int,
    names: Map<Int, String>,
    hookArgResolver: HookArgResolver?,
) {
    val name = names.getValue(index)
    sb.append("[[component]]\n")
    sb.append("name = \"").append(name).append("\"\n")
    sb.append("type = \"").append(typeToString(comp.type)).append("\"\n")

    val parentIndex = (comp.layer shr CHILD_BIT_OFFSET) and CHILD_BIT_MASK
    if (parentIndex != 0) {
        sb.append("parent = \"").append(names.getValue(parentIndex)).append("\"\n")
    }

    if (comp.x != 0) sb.append("x = ").append(comp.x).append('\n')
    if (comp.y != 0) sb.append("y = ").append(comp.y).append('\n')
    if (comp.width != 0) sb.append("width = ").append(comp.width).append('\n')
    if (comp.height != 0) sb.append("height = ").append(comp.height).append('\n')
    if (comp.xMode != 0) sb.append("x_mode = ").append(comp.xMode).append('\n')
    if (comp.yMode != 0) sb.append("y_mode = ").append(comp.yMode).append('\n')
    if (comp.widthMode != 0) sb.append("width_mode = ").append(comp.widthMode).append('\n')
    if (comp.heightMode != 0) sb.append("height_mode = ").append(comp.heightMode).append('\n')
    if (comp.clientCode != 0) sb.append("content_type = ").append(comp.clientCode).append('\n')
    if (comp.hide) sb.append("hide = true\n")
    if (comp.buttonType != 0) sb.append("button_type = ").append(comp.buttonType).append('\n')
    if (comp.mouseOverRedirect != -1) sb.append("mouse_over_redirect = ").append(comp.mouseOverRedirect).append('\n')
    if (comp.opBase.isNotEmpty()) sb.append("op_base = \"").append(comp.opBase).append("\"\n")
    if (comp.targetVerb.isNotEmpty()) sb.append("target_verb = \"").append(comp.targetVerb).append("\"\n")
    if (comp.targetBase.isNotEmpty()) sb.append("target_base = \"").append(comp.targetBase).append("\"\n")
    if (comp.buttonText.isNotEmpty() && comp.buttonText != "Ok") {
        sb.append("button_text = \"").append(comp.buttonText).append("\"\n")
    }
    if (comp.dragDeadZone != 0) sb.append("drag_dead_zone = ").append(comp.dragDeadZone).append('\n')
    if (comp.dragDeadTime != 0) sb.append("drag_dead_time = ").append(comp.dragDeadTime).append('\n')
    if (comp.draggableBehavior) sb.append("draggable_behavior = true\n")

    val options = comp.op.filter { it.isNotBlank() }
    if (options.isNotEmpty()) {
        sb.append("options = [").append(options.joinToString(", ") { "\"$it\"" }).append("]\n")
    }
    val optionDerivedEvents = if (options.isNotEmpty()) 2 else 0
    if (comp.events != optionDerivedEvents) {
        val tokens = eventTokens(comp.events)
        sb.append("events = [").append(tokens.joinToString(", ") { "\"$it\"" }).append("]\n")
    }
    appendHook(sb, "on_load", comp.onLoad, hookArgResolver)
    appendHook(sb, "on_op", comp.onOp, hookArgResolver)
    appendHook(sb, "on_click", comp.onClick, hookArgResolver)
    appendHook(sb, "on_click_repeat", comp.onClickRepeat, hookArgResolver)
    appendHook(sb, "on_release", comp.onRelease, hookArgResolver)
    appendHook(sb, "on_hold", comp.onHold, hookArgResolver)
    appendHook(sb, "on_mouse_over", comp.onMouseOver, hookArgResolver)
    appendHook(sb, "on_mouse_repeat", comp.onMouseRepeat, hookArgResolver)
    appendHook(sb, "on_mouse_leave", comp.onMouseLeave, hookArgResolver)
    appendHook(sb, "on_drag", comp.onDrag, hookArgResolver)
    appendHook(sb, "on_drag_complete", comp.onDragComplete, hookArgResolver)
    appendHook(sb, "on_target_enter", comp.onTargetEnter, hookArgResolver)
    appendHook(sb, "on_target_leave", comp.onTargetLeave, hookArgResolver)
    appendHook(sb, "on_var_transmit", comp.onVarTransmit, hookArgResolver)
    appendHook(sb, "on_inv_transmit", comp.onInvTransmit, hookArgResolver)
    appendHook(sb, "on_stat_transmit", comp.onStatTransmit, hookArgResolver)
    appendHook(sb, "on_timer", comp.onTimer, hookArgResolver)
    appendHook(sb, "on_scroll_wheel", comp.onScrollWheel, hookArgResolver)
    appendIntArray(sb, "on_var_transmit_list", comp.onVarTransmitList)
    appendIntArray(sb, "on_inv_transmit_list", comp.onInvTransmitList)
    appendIntArray(sb, "on_stat_transmit_list", comp.onStatTransmitList)

    when (comp.type) {
        0 -> {
            if (comp.scrollWidth != 0) sb.append("scroll_width = ").append(comp.scrollWidth).append('\n')
            if (comp.scrollHeight != 0) sb.append("scroll_height = ").append(comp.scrollHeight).append('\n')
            if (comp.noClickThrough) sb.append("no_click_through = true\n")
        }
        4 -> {
            if (comp.text.isNotEmpty()) sb.append("text = \"").append(comp.text).append("\"\n")
            if (comp.secondaryText.isNotEmpty()) sb.append("secondary_text = \"").append(comp.secondaryText).append("\"\n")
            sb.append("font = \"").append(fontToString(comp.textFont)).append("\"\n")
            if (comp.textLineHeight != 0) sb.append("line_height = ").append(comp.textLineHeight).append('\n')
            sb.append("h_align = \"").append(alignToString(comp.textAlignH)).append("\"\n")
            sb.append("v_align = \"").append(alignToString(comp.textAlignV)).append("\"\n")
            if (!comp.textShadow) sb.append("text_shadow = false\n")
            if (comp.colour1 != 0) sb.append("color = \"#").append("%06X".format(comp.colour1)).append("\"\n")
            if (comp.mouseOverColour1 != 0) {
                sb.append("mouse_over_color = \"#").append("%06X".format(comp.mouseOverColour1)).append("\"\n")
            }
            if (comp.mouseOverColour2 != 0) {
                sb.append("mouse_over_secondary_color = \"#").append("%06X".format(comp.mouseOverColour2)).append("\"\n")
            }
        }
        5 -> {
            if (comp.graphic != -1) appendIntOrConstant(sb, "sprite_id", comp.graphic, *SPRITE_TABLES)
            if (comp.secondaryGraphic != -1) {
                appendIntOrConstant(sb, "secondary_sprite_id", comp.secondaryGraphic, *SPRITE_TABLES)
            }
            if (comp.angle2d != 0) appendIntOrConstant(sb, "texture_id", comp.angle2d, *TEXTURE_TABLES)
            if (comp.tiling) sb.append("sprite_tiling = true\n")
            if (comp.outline != 0) sb.append("border_type = ").append(comp.outline).append('\n')
            if (comp.graphicShadow != 0) {
                sb.append("shadow_color = \"#").append("%06X".format(comp.graphicShadow)).append("\"\n")
            }
            if (comp.vFlip) sb.append("flip_v = true\n")
            if (comp.hFlip) sb.append("flip_h = true\n")
            if (comp.trans1 != 0) sb.append("opacity = ").append(comp.trans1).append('\n')
        }
        3 -> {
            if (comp.colour1 != 0) sb.append("color = \"#").append("%06X".format(comp.colour1)).append("\"\n")
            if (comp.colour2 != 0) sb.append("secondary_color = \"#").append("%06X".format(comp.colour2)).append("\"\n")
            if (comp.fill) sb.append("filled = true\n")
            if (comp.trans1 != 0) sb.append("opacity = ").append(comp.trans1).append('\n')
        }
        9 -> {
            if (comp.lineWid != 1) sb.append("line_width = ").append(comp.lineWid).append('\n')
            if (comp.colour1 != 0) sb.append("color = \"#").append("%06X".format(comp.colour1)).append("\"\n")
            if (comp.colour2 != 0) sb.append("secondary_color = \"#").append("%06X".format(comp.colour2)).append("\"\n")
            if (comp.lineDirection) sb.append("line_direction = true\n")
        }
        6 -> {
            if (comp.model != -1) appendIntOrConstant(sb, "model_id", comp.model, *MODEL_TABLES)
            if (comp.modelKind != 1) sb.append("model_kind = ").append(comp.modelKind).append('\n')
            if (comp.secondaryModel != -1) {
                appendIntOrConstant(sb, "secondary_model_id", comp.secondaryModel, *MODEL_TABLES)
            }
            if (comp.secondaryModelKind != 1) sb.append("secondary_model_kind = ").append(comp.secondaryModelKind).append('\n')
            if (comp.modelX != 0) sb.append("offset_x2d = ").append(comp.modelX).append('\n')
            if (comp.modelY != 0) sb.append("offset_y2d = ").append(comp.modelY).append('\n')
            if (comp.modelAngleX != 0) sb.append("rotation_x = ").append(comp.modelAngleX).append('\n')
            if (comp.modelAngleY != 0) sb.append("rotation_y = ").append(comp.modelAngleY).append('\n')
            if (comp.modelAngleZ != 0) sb.append("rotation_z = ").append(comp.modelAngleZ).append('\n')
            if (comp.modelZoom != 100) sb.append("model_zoom = ").append(comp.modelZoom).append('\n')
            if (comp.modelAnim != -1) appendIntOrConstant(sb, "animation", comp.modelAnim, *ANIMATION_TABLES)
            if (comp.secondaryModelAnim != -1) {
                appendIntOrConstant(sb, "secondary_animation", comp.secondaryModelAnim, *ANIMATION_TABLES)
            }
            if (comp.modelObjWidth != 0) sb.append("model_height_override = ").append(comp.modelObjWidth).append('\n')
            if (comp.modelOrthog) sb.append("orthogonal = true\n")
        }
    }
}

private fun appendHook(sb: StringBuilder, key: String, hook: Array<Any>?, hookArgResolver: HookArgResolver?) {
    if (hook == null) return
    appendHookLine(sb, key, hook, hookArgResolver)
}

private fun appendHookLine(sb: StringBuilder, key: String, hook: Array<Any>, hookArgResolver: HookArgResolver?) {
    if (hook.isEmpty()) return
    val scriptId = hook[0] as? Int
    val rendered = hook.mapIndexed { index, value ->
        when (value) {
            is Int -> if (index == 0) {
                reverseConstant(value, SCRIPT_TABLES)?.let { "\"$it\"" } ?: value.toString()
            } else {
                val resolved = scriptId?.let { hookArgResolver?.invoke(it, index - 1, value) }
                when {
                    resolved != null -> "\"$resolved\""
                    value >= MIN_PACKED_COMPONENT_ID -> reverseConstant(value, COMPONENT_TABLES)?.let { "\"$it\"" } ?: value.toString()
                    else -> value.toString()
                }
            }
            is String -> "\"${value.replace("\\", "\\\\").replace("\"", "\\\"")}\""
            else -> value.toString()
        }
    }
    sb.append(key).append(" = [").append(rendered.joinToString(", ")).append("]\n")
}

private fun eventTokens(events: Int): List<String> {
    if (events == 0) return emptyList()
    var remaining = events
    val tokens = mutableListOf<String>()
    for (flag in EVENT_FLAGS) {
        val bit = flag.bitmask.toInt()
        if (remaining and bit != 0) {
            tokens += flag.name
            remaining = remaining and bit.inv()
        }
    }
    if (remaining != 0) tokens += "0x" + remaining.toString(16)
    return tokens
}

private fun appendIntArray(sb: StringBuilder, key: String, values: IntArray?) {
    if (values == null || values.isEmpty()) return
    sb.append(key).append(" = [").append(values.joinToString(", ")).append("]\n")
}

private fun appendIntOrConstant(sb: StringBuilder, key: String, value: Int, vararg tables: String) {
    val constant = reverseConstant(value, tables)
    if (constant != null) {
        sb.append(key).append(" = \"").append(constant).append("\"\n")
    } else {
        sb.append(key).append(" = ").append(value).append('\n')
    }
}

private val BRACKETED_SYM_NAME = Regex("^\\[\\w+,(.+)]$")

private fun reverseConstant(id: Int, tables: Array<out String>): String? {
    for (table in tables) {
        if (table !in ConstantProvider.types) continue
        val resolved = runCatching { ConstantProvider.getReverseMapping(table, id) }.getOrNull() ?: continue

        val suffix = resolved.substringAfter('.')
        val bracketed = BRACKETED_SYM_NAME.matchEntire(suffix) ?: return resolved

        val cleanName = bracketed.groupValues[1]
        ConstantProvider.putMapping(table, cleanName, id)
        return "$table.$cleanName"
    }
    return null
}

private fun typeToString(type: Int): String = when (type) {
    0 -> "layer"
    3 -> "rectangle"
    4 -> "text"
    5 -> "graphic"
    6 -> "model"
    9 -> "line"
    12 -> "input"
    else -> "layer"
}

private fun alignToString(value: Int): String = when (value) {
    0 -> "left"
    2 -> "right"
    else -> "center"
}

private fun fontToString(value: Int): String = when (value) {
    494 -> "small"
    496 -> "bold"
    497 -> "large"
    else -> "regular"
}
