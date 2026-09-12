package dev.openrune.cache.gameval.impl

import dev.openrune.cache.gameval.GameValElement

data class Interface(
    override val name: String,
    override val id: Int,
    val components: List<InterfaceComponent> = emptyList()
) : GameValElement(name, id) {

    /** Child id index, built on first use. */
    private val componentsById: Map<Int, InterfaceComponent> by lazy {
        HashMap<Int, InterfaceComponent>(if (components.size < 3) 4 else (components.size / 0.75f).toInt() + 1)
            .apply {
                // First-wins.
                for (component in components) {
                    putIfAbsent(component.id, component)
                }
            }
    }

    /** The first child component with [id], or null when this interface has no such child. */
    fun component(id: Int): InterfaceComponent? = componentsById[id]

    data class InterfaceComponent(
        override val name: String,
        override val id: Int,
        val linkedInterfaceID: Int
    ) : GameValElement(name, id) {
        enum class FormatMode { DEFAULT, PACKED }

        val packed =  (linkedInterfaceID and 0xFFFF shl 16) or (id and 0xFFFF)

        fun toFullString(mode: FormatMode = FormatMode.DEFAULT): String =
            when (mode) {
                FormatMode.DEFAULT -> "$name:$id"
                FormatMode.PACKED -> "$name:${packed}"
            }

        override fun toFullString() = toFullString(FormatMode.DEFAULT)

    }

}