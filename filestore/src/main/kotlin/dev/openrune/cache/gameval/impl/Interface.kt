package dev.openrune.cache.gameval.impl

import dev.openrune.cache.gameval.GameValElement

data class Interface(
    override val name: String,
    override val id: Int,
    val components: List<InterfaceComponent> = emptyList()
) : GameValElement(name, id) {

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