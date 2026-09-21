package dev.openrune.definition.type

import dev.openrune.definition.Definition

data class QuickChatCategoryRef(val id: Int, val priority: Int)
data class QuickChatPhraseRef(val id: Int, val priority: Int)

data class QuickChatCategoryType(
    override var id: Int = -1,
    var description: String? = null,
    var subCategories: List<QuickChatCategoryRef> = emptyList(),
    var phrases: List<QuickChatPhraseRef> = emptyList(),
    var searchable: Boolean = false
) : Definition
