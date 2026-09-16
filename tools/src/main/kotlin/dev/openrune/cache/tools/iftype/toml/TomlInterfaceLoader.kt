package dev.openrune.cache.tools.iftype.toml

import dev.openrune.cache.filestore.definition.InterfaceType
import dev.openrune.cache.tools.iftype.dsl.BaseComponent
import dev.openrune.cache.tools.iftype.dsl.InterfaceBuilder
import dev.openrune.cache.tools.iftype.dsl.buildInterface
import dev.openrune.cache.tools.iftype.dsl.impl.Graphic
import dev.openrune.cache.tools.iftype.dsl.impl.Layer
import dev.openrune.cache.tools.iftype.dsl.impl.Line
import dev.openrune.cache.tools.iftype.dsl.impl.Model
import dev.openrune.cache.tools.iftype.dsl.impl.Rectangle
import dev.openrune.cache.tools.iftype.dsl.impl.Text
import dev.openrune.cache.tools.iftype.dsl.impl.TextAlignment
import dev.openrune.cache.tools.iftype.dsl.impl.FontType
import dev.openrune.cache.tools.iftype.dsl.impl.graphic
import dev.openrune.cache.tools.iftype.dsl.impl.input
import dev.openrune.cache.tools.iftype.dsl.impl.layer
import dev.openrune.cache.tools.iftype.dsl.impl.line
import dev.openrune.cache.tools.iftype.dsl.impl.model
import dev.openrune.cache.tools.iftype.dsl.impl.rectangle
import dev.openrune.cache.tools.iftype.dsl.impl.text
import dev.openrune.definition.constants.ConstantProvider
import dev.openrune.definition.type.widget.IfEvent
import dev.openrune.toml.TomlMapper
import dev.openrune.toml.decode
import dev.openrune.toml.model.TomlValue
import dev.openrune.toml.rsconfig.applyConstantProviderIfNeeded
import dev.openrune.toml.rsconfig.applyTokenizedReplacementsIfNeeded
import dev.openrune.toml.serialization.from
import java.nio.file.Path
import kotlin.io.path.readText

fun validateInterfaceToml(file: TomlInterfaceFile) {
    require(file.`interface`.size == 1) {
        "Expected exactly one [[interface]] block per file, found ${file.`interface`.size}."
    }
    val header = file.`interface`.single()

    require(header.name > 0) { "Interface 'name' must resolve to a valid id (got ${header.name})." }
    require(file.component.isNotEmpty() || file.edit.isNotEmpty()) {
        "Interface '${header.name}' declares no [[component]] or [[edit]] entries."
    }
    require(file.edit.isEmpty() || header.inherit != null) {
        "Interface '${header.name}' declares [[edit]] entries but no 'inherit' — edit() requires inherit()."
    }

    val editNames = mutableSetOf<String>()
    for (e in file.edit) {
        require(e.name.isNotBlank()) { "An [[edit]] in '${header.name}' is missing its 'name'." }
        require(editNames.add(e.name)) { "Duplicate edit '${e.name}' in '${header.name}'." }
    }

    val seen = mutableSetOf<String>()
    for (c in file.component) {
        require(c.name.isNotBlank()) { "A component in '${header.name}' is missing its 'name'." }
        require(c.name != "universe") {
            "Component 'universe' in '${header.name}' — that name is reserved for the auto-generated root."
        }
        require(seen.add(c.name)) { "Duplicate component name '${c.name}' in '${header.name}'." }
        require(c.type in VALID_COMPONENT_TYPES) {
            "Component '${c.name}' has unknown type '${c.type}'. Valid types: ${VALID_COMPONENT_TYPES.sorted().joinToString(", ")}."
        }
    }

    val byName = file.component.associateBy { it.name }
    for (c in file.component) {
        val parentName = c.parent ?: continue
        val parent = byName[parentName]
            ?: error(
                "Component '${c.name}' declares parent = \"$parentName\", but no component with that name " +
                    "exists in '${header.name}'. Available: ${byName.keys.sorted()}",
            )
        require(parent.type == "layer") {
            "Component '${c.name}' declares parent = \"$parentName\", but '$parentName' is type " +
                "'${parent.type}', not 'layer' — only layer components can hold children."
        }
        require(c.type != "input") {
            "Component '${c.name}' is type 'input' with parent = \"$parentName\" — input components are only " +
                "supported at the top level (the interface DSL has no nested input())."
        }
    }

    for (c in file.component) {
        var current: TomlComponent? = c
        val path = mutableListOf<String>()
        while (current != null) {
            val name = current.name
            require(name !in path) {
                "Cycle in component parent chain: ${(path + name).joinToString(" -> ")}"
            }
            path += name
            current = current.parent?.let { byName[it] }
        }
    }
}

fun buildInterfaceFromToml(file: TomlInterfaceFile): InterfaceType {
    validateInterfaceToml(file)
    val header = file.`interface`.single()

    val displayName = resolveInterfaceName(header.name) ?: header.name.toString()

    val childrenByParent = file.component.filter { it.parent != null }.groupBy { it.parent!! }
    val roots = file.component.filter { it.parent == null }

    val rootLayerOverride = if (header.inherit != null) (header.name shl 16) else null

    return buildInterface(id = header.name, interfaceName = displayName, width = header.width, height = header.height) {
        if (header.on_load.isNotEmpty()) onLoadListener { header.on_load.toHookArray() }
        header.inherit?.let { inheritId ->
            val inheritName = resolveInterfaceName(inheritId)
                ?: error("Could not resolve 'inherit' id $inheritId back to an interface name.")
            inherit("interface.$inheritName")
        }
        roots.forEach { placeAtRoot(this, it, childrenByParent, rootLayerOverride) }
        file.edit.forEach { applyEdit(this, it) }
    }
}

private const val INTERFACE_TABLE = "interface"

private fun resolveInterfaceName(id: Int): String? =
    runCatching { ConstantProvider.getReverseMapping(INTERFACE_TABLE, id) }.getOrNull()?.substringAfter('.')

private fun applyEdit(host: InterfaceBuilder, toml: TomlEdit) {
    host.edit(toml.name) {
        toml.x?.let { x { it } }
        toml.y?.let { y { it } }
        toml.width?.let { width { it } }
        toml.height?.let { height { it } }
        toml.sprite_id?.let { spriteId { it } }
        toml.hide?.let { hide { it } }
        toml.scroll_width?.let { scrollWidth { it } }
        toml.scroll_height?.let { scrollHeight { it } }
    }
}

private fun List<Any>.toHookArray(): Array<Any> = map { if (it is Long) it.toInt() else it }.toTypedArray()

fun loadInterfaceToml(
    path: Path,
    mapper: TomlMapper,
    cs2ParamTypes: Map<String, List<String>> = emptyMap(),
): InterfaceType {
    val raw = path.readText()
    if (cs2ParamTypes.isNotEmpty()) {
        val problems = validateCs2HookArgs(raw, cs2ParamTypes)
        require(problems.isEmpty()) {
            "CS2 hook argument validation failed for '${path.fileName}':\n" +
                problems.joinToString("\n") { "  - $it" }
        }
    }
    val selfResolved = resolveSelfComponentRefs(raw)
    val normalized = normalizeScriptRefs(selfResolved)
    val preprocessed = mapper.applyConstantProviderIfNeeded(mapper.applyTokenizedReplacementsIfNeeded(normalized))
    val file = mapper.decode<TomlInterfaceFile>(TomlValue.from(preprocessed))
    return buildInterfaceFromToml(file)
}

private fun normalizeScriptRefs(rawToml: String): String {
    if (SCRIPT_NAME_TABLES.none { rawToml.contains("\"$it.") }) return rawToml
    return QUOTED_STRING.replace(rawToml) { match ->
        val token = match.groupValues[1]
        val table = token.substringBefore('.', missingDelimiterValue = "")
        if (table !in SCRIPT_NAME_TABLES) return@replace match.value

        val name = token.substringAfter('.', missingDelimiterValue = "")
        if (name.startsWith('[') || ConstantProvider.getMappingOrNull(token) != null) return@replace match.value

        val bracketed = "$table.[$table,$name]"
        if (ConstantProvider.getMappingOrNull(bracketed) != null) "\"$bracketed\"" else match.value
    }
}

private val INTERFACE_NAME_LINE = Regex("""^name\s*=\s*"interface\.([A-Za-z0-9_]+)"\s*$""")
private val COMPONENT_NAME_LINE = Regex("""^name\s*=\s*"([^"]*)"\s*$""")
private val PARENT_LINE = Regex("""^parent\s*=\s*"([^"]*)"\s*$""")
private val COMPONENT_BLOCK_HEADER = Regex("""^\[\[component]]\s*$""")

private fun resolveSelfComponentRefs(rawToml: String): String {
    val interfaceName = rawToml.lineSequence()
        .map { it.trim() }
        .firstNotNullOfOrNull { INTERFACE_NAME_LINE.matchEntire(it)?.groupValues?.get(1) }
        ?: return rawToml
    val interfaceId = ConstantProvider.getMappingOrNull("interface.$interfaceName") ?: return rawToml

    data class Node(val name: String, val parent: String?)

    val nodes = mutableListOf<Node>()
    var inComponentBlock = false
    var pendingName: String? = null
    var pendingParent: String? = null

    fun flush() {
        pendingName?.let { nodes += Node(it, pendingParent) }
        pendingName = null
        pendingParent = null
    }

    rawToml.lineSequence().forEach { rawLine ->
        val line = rawLine.trim()
        if (COMPONENT_BLOCK_HEADER.matches(line)) {
            flush()
            inComponentBlock = true
            return@forEach
        }
        if (!inComponentBlock) return@forEach
        COMPONENT_NAME_LINE.matchEntire(line)?.let { pendingName = it.groupValues[1] }
        PARENT_LINE.matchEntire(line)?.let { pendingParent = it.groupValues[1] }
    }
    flush()
    if (nodes.isEmpty()) return rawToml

    val childrenByParent = nodes.filter { it.parent != null }.groupBy { it.parent!! }
    val roots = nodes.filter { it.parent == null }
    val indexByName = mutableMapOf<String, Int>()
    var nextIndex = 1

    fun assign(node: Node) {
        indexByName[node.name] = nextIndex++
        childrenByParent[node.name].orEmpty().forEach(::assign)
    }
    roots.forEach(::assign)

    var result = rawToml
    indexByName.forEach { (name, index) ->
        val packed = (interfaceId shl 16) or index
        result = result.replace("\"component.$interfaceName:$name\"", packed.toString())
    }
    return result
}

private fun placeAtRoot(
    host: InterfaceBuilder,
    node: TomlComponent,
    childrenByParent: Map<String, List<TomlComponent>>,
    rootLayerOverride: Int?,
) {
    when (node.type) {
        "layer" -> host.layer(node.name) {
            configureLayer(this, node)
            rootLayerOverride?.let { layer { it } }
            childrenByParent[node.name].orEmpty().forEach { placeInLayer(this, it, childrenByParent) }
        }
        "text" -> host.text(node.name) { configureText(this, node); rootLayerOverride?.let { layer { it } } }
        "graphic" -> host.graphic(node.name) { configureGraphic(this, node); rootLayerOverride?.let { layer { it } } }
        "rectangle" -> host.rectangle(node.name) { configureRectangle(this, node); rootLayerOverride?.let { layer { it } } }
        "line" -> host.line(node.name) { configureLine(this, node); rootLayerOverride?.let { layer { it } } }
        "model" -> host.model(node.name) { configureModel(this, node); rootLayerOverride?.let { layer { it } } }
        "input" -> host.input(node.name) {
            configureCommon(this, node)
            applyExplicitEvents(this, node)
            rootLayerOverride?.let { layer { it } }
        }
        else -> error("Unknown component type '${node.type}' for '${node.name}'")
    }
}

private fun placeInLayer(
    host: Layer.LayerComponent,
    node: TomlComponent,
    childrenByParent: Map<String, List<TomlComponent>>,
) {
    when (node.type) {
        "layer" -> host.layer(node.name) {
            configureLayer(this, node)
            childrenByParent[node.name].orEmpty().forEach { placeInLayer(this, it, childrenByParent) }
        }
        "text" -> host.text(node.name) { configureText(this, node) }
        "graphic" -> host.graphic(node.name) { configureGraphic(this, node) }
        "rectangle" -> host.rectangle(node.name) { configureRectangle(this, node) }
        "line" -> host.line(node.name) { configureLine(this, node) }
        "model" -> host.model(node.name) { configureModel(this, node) }
        else -> error(
            "Component type '${node.type}' for '${node.name}' cannot be nested inside a layer " +
                "(only layer/text/graphic/rectangle/line/model are supported as children).",
        )
    }
}

private fun configureCommon(bld: BaseComponent, node: TomlComponent) {
    bld.position { node.x to node.y }
    bld.size { node.width to node.height }
    bld.xMode { node.x_mode }
    bld.yMode { node.y_mode }
    bld.widthMode { node.width_mode }
    bld.heightMode { node.height_mode }
    bld.contentType { node.content_type }
    bld.hide { node.hide }
    if (node.button_type != 0) bld.buttonType { node.button_type }
    if (node.mouse_over_redirect != -1) bld.mouseOverRedirect { node.mouse_over_redirect }
    if (node.op_base.isNotEmpty()) bld.opBase { node.op_base }
    if (node.target_verb.isNotEmpty()) bld.targetVerb { node.target_verb }
    if (node.target_base.isNotEmpty()) bld.targetBase { node.target_base }
    if (node.button_text.isNotEmpty()) bld.buttonText { node.button_text }
    if (node.drag_dead_zone != 0) bld.dragDeadZone { node.drag_dead_zone }
    if (node.drag_dead_time != 0) bld.dragDeadTime { node.drag_dead_time }
    if (node.draggable_behavior) bld.draggableBehavior { node.draggable_behavior }

    if (node.on_load.isNotEmpty()) bld.onLoadListener { node.on_load.toHookArray() }
    if (node.on_op.isNotEmpty()) bld.onOpListener { node.on_op.toHookArray() }
    if (node.on_click.isNotEmpty()) bld.onClickListener { node.on_click.toHookArray() }
    if (node.on_click_repeat.isNotEmpty()) bld.onClickRepeatListener { node.on_click_repeat.toHookArray() }
    if (node.on_release.isNotEmpty()) bld.onReleaseListener { node.on_release.toHookArray() }
    if (node.on_hold.isNotEmpty()) bld.onHoldListener { node.on_hold.toHookArray() }
    if (node.on_mouse_over.isNotEmpty()) bld.onMouseOverListener { node.on_mouse_over.toHookArray() }
    if (node.on_mouse_repeat.isNotEmpty()) bld.onMouseRepeatListener { node.on_mouse_repeat.toHookArray() }
    if (node.on_mouse_leave.isNotEmpty()) bld.onMouseLeaveListener { node.on_mouse_leave.toHookArray() }
    if (node.on_drag.isNotEmpty()) bld.onDragListener { node.on_drag.toHookArray() }
    if (node.on_drag_complete.isNotEmpty()) bld.onDragCompleteListener { node.on_drag_complete.toHookArray() }
    if (node.on_target_enter.isNotEmpty()) bld.onTargetEnterListener { node.on_target_enter.toHookArray() }
    if (node.on_target_leave.isNotEmpty()) bld.onTargetLeaveListener { node.on_target_leave.toHookArray() }
    if (node.on_var_transmit.isNotEmpty()) bld.onVarTransmitListener { node.on_var_transmit.toHookArray() }
    if (node.on_inv_transmit.isNotEmpty()) bld.onInvTransmitListener { node.on_inv_transmit.toHookArray() }
    if (node.on_stat_transmit.isNotEmpty()) bld.onStatTransmitListener { node.on_stat_transmit.toHookArray() }
    if (node.on_timer.isNotEmpty()) bld.onTimerListener { node.on_timer.toHookArray() }
    if (node.on_scroll_wheel.isNotEmpty()) bld.onScrollWheelListener { node.on_scroll_wheel.toHookArray() }

    if (node.on_var_transmit_list.isNotEmpty()) bld.onVarTransmitList { node.on_var_transmit_list.toIntArray() }
    if (node.on_inv_transmit_list.isNotEmpty()) bld.onInvTransmitList { node.on_inv_transmit_list.toIntArray() }
    if (node.on_stat_transmit_list.isNotEmpty()) bld.onStatTransmitList { node.on_stat_transmit_list.toIntArray() }
}

private fun applyExplicitEvents(bld: BaseComponent, node: TomlComponent) {
    if (node.events.isEmpty()) return
    val bitmask = node.events.fold(0) { acc, token ->
        val named = IfEvent.entries.firstOrNull { it.name == token }?.bitmask?.toInt()
        val bits = named ?: token.removePrefix("0x").toIntOrNull(16)
            ?: error(
                "Unknown event '$token'. Use an IfEvent name (${IfEvent.entries.joinToString(", ") { it.name }}) " +
                    "or a 0x-prefixed hex bitmask.",
            )
        acc or bits
    }
    bld.events { bitmask }
}

private fun configureLayer(bld: Layer.LayerComponent, node: TomlComponent) {
    configureCommon(bld, node)
    bld.scrollWidth { node.scroll_width }
    bld.scrollHeight { node.scroll_height }
    bld.noClickThrough { node.no_click_through }
    node.options.forEach { bld.addOption(it) }
    applyExplicitEvents(bld, node)
}

private fun configureText(bld: Text.TextComponent, node: TomlComponent) {
    configureCommon(bld, node)
    bld.display { node.text }
    if (node.secondary_text.isNotEmpty()) bld.secondaryDisplay { node.secondary_text }
    bld.font { fontFromString(node.font) }
    bld.lineHeight { node.line_height }
    bld.horizontalAlignment { alignFromString(node.h_align) }
    bld.verticalAlignment { alignFromString(node.v_align) }
    bld.textShadowed { node.text_shadow }
    if (node.color.isNotBlank()) bld.color(node.color)
    if (node.mouse_over_color.isNotBlank()) bld.mouseOverColor(parseColor(node.mouse_over_color))
    if (node.mouse_over_secondary_color.isNotBlank()) bld.mouseOverSecondaryColor(parseColor(node.mouse_over_secondary_color))
    node.options.forEach { bld.addOption(it) }
    applyExplicitEvents(bld, node)
}

private fun configureGraphic(bld: Graphic.GraphicComponent, node: TomlComponent) {
    configureCommon(bld, node)
    if (node.sprite_id != -1) bld.spriteId { node.sprite_id }
    if (node.secondary_sprite_id != -1) bld.secondarySpriteId { node.secondary_sprite_id }
    bld.textureId { node.texture_id }
    bld.spriteTiling { node.sprite_tiling }
    bld.borderType { node.border_type }
    if (node.shadow_color.isNotBlank()) bld.shadowColor(parseColor(node.shadow_color))
    bld.flippedVertically { node.flip_v }
    bld.flippedHorizontally { node.flip_h }
    bld.opacity { node.opacity }
    node.options.forEach { bld.addOption(it) }
    applyExplicitEvents(bld, node)
}

private fun configureRectangle(bld: Rectangle.RectangleComponent, node: TomlComponent) {
    configureCommon(bld, node)
    if (node.color.isNotBlank()) bld.color(parseColor(node.color))
    if (node.secondary_color.isNotBlank()) bld.secondaryColor(parseColor(node.secondary_color))
    bld.filled { node.filled }
    bld.opacity { node.opacity }
    applyExplicitEvents(bld, node)
}

private fun configureLine(bld: Line.LineComponent, node: TomlComponent) {
    configureCommon(bld, node)
    bld.lineWidth { node.line_width }
    if (node.color.isNotBlank()) bld.color(parseColor(node.color))
    if (node.secondary_color.isNotBlank()) bld.secondaryColor(parseColor(node.secondary_color))
    bld.lineDirection { node.line_direction }
    applyExplicitEvents(bld, node)
}

private fun configureModel(bld: Model.ModelComponent, node: TomlComponent) {
    configureCommon(bld, node)
    bld.modelId { node.model_id }
    bld.modelKind { node.model_kind }
    if (node.secondary_model_id != -1) bld.secondaryModelId { node.secondary_model_id }
    bld.secondaryModelKind { node.secondary_model_kind }
    bld.offsetX2d { node.offset_x2d }
    bld.offsetY2d { node.offset_y2d }
    bld.rotationX { node.rotation_x }
    bld.rotationY { node.rotation_y }
    bld.rotationZ { node.rotation_z }
    bld.modelZoom { node.model_zoom }
    bld.animation { node.animation }
    if (node.secondary_animation != -1) bld.secondaryAnimation { node.secondary_animation }
    bld.modelHeightOverride { node.model_height_override }
    bld.orthogonal { node.orthogonal }
    applyExplicitEvents(bld, node)
}

private fun parseColor(value: String): Int = if (value.isBlank()) 0 else value.removePrefix("#").toInt(16)

private fun alignFromString(value: String): TextAlignment = when (value.lowercase()) {
    "left" -> TextAlignment.LEFT
    "center", "centre" -> TextAlignment.CENTER
    "right" -> TextAlignment.RIGHT
    else -> error("Unknown alignment '$value'. Valid values: left, center, right.")
}

private fun fontFromString(value: String): FontType = when (value.lowercase()) {
    "small" -> FontType.FONT_SMALL
    "regular" -> FontType.FONT_REGULAR
    "bold" -> FontType.FONT_BOLD
    "large" -> FontType.FONT_LARGE_STYLE
    else -> error("Unknown font '$value'. Valid values: small, regular, bold, large.")
}
