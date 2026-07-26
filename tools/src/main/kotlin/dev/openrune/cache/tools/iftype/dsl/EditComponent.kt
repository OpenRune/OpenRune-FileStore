package dev.openrune.cache.tools.iftype.dsl

class EditComponent(val name: String) {
    var x: Int? = null
        private set
    var y: Int? = null
        private set
    var width: Int? = null
        private set
    var height: Int? = null
        private set
    var graphic: Int? = null
        private set
    var hide: Boolean? = null
        private set
    var scrollWidth: Int? = null
        private set
    var scrollHeight: Int? = null
        private set

    fun position(block: () -> Pair<Int, Int>) {
        val (nx, ny) = block()
        x = nx
        y = ny
    }

    fun position(pair: Pair<Int, Int>) {
        x = pair.first
        y = pair.second
    }

    fun size(block: () -> Pair<Int, Int>) {
        val (w, h) = block()
        width = w
        height = h
    }

    fun size(pair: Pair<Int, Int>) {
        width = pair.first
        height = pair.second
    }

    fun width(block: () -> Int) {
        width = block()
    }

    fun height(block: () -> Int) {
        height = block()
    }

    fun spriteId(block: () -> Int) {
        graphic = block()
    }

    fun hide(block: () -> Boolean) {
        hide = block()
    }

    fun scrollWidth(block: () -> Int) {
        scrollWidth = block()
    }

    fun scrollHeight(block: () -> Int) {
        scrollHeight = block()
    }
}

object InterfaceEdits {
    private val byId = mutableMapOf<Int, List<EditComponent>>()

    fun register(interfaceId: Int, edits: List<EditComponent>) {
        if (edits.isEmpty()) return
        byId[interfaceId] = edits
    }

    fun take(interfaceId: Int): List<EditComponent> = byId.remove(interfaceId).orEmpty()
}

object InterfaceFrom {
    private val byId = mutableMapOf<Int, Map<String, String>>()

    fun register(interfaceId: Int, fromByName: Map<String, String>) {
        if (fromByName.isEmpty()) return
        byId[interfaceId] = fromByName
    }

    fun take(interfaceId: Int): Map<String, String> = byId.remove(interfaceId).orEmpty()
}
