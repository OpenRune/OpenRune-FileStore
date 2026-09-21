package dev.openrune.rs2

import dev.openrune.Rs2Cache
import dev.openrune.definition.type.QuickChatCategoryType
import dev.openrune.definition.type.QuickChatPhraseType

/** Loads both quickchat categories and phrases, and can print the category/phrase hierarchy as a tree. */
class QuickChatDecoder {

    val categories: MutableMap<Int, QuickChatCategoryType> = mutableMapOf()
    val phrases: MutableMap<Int, QuickChatPhraseType> = mutableMapOf()

    private val categoryDecoder = QuickChatCategoryDecoder()
    private val phraseDecoder = QuickChatPhraseDecoder()

    fun load(cache: Rs2Cache) {
        categories.clear()
        phrases.clear()
        categoryDecoder.load(cache, categories)
        phraseDecoder.load(cache, phrases)
    }

    /** Loads only the given category/phrase ids instead of everything. */
    fun load(cache: Rs2Cache, categoryIds: Iterable<Int>, phraseIds: Iterable<Int>) {
        categoryDecoder.load(cache, categories, categoryIds)
        phraseDecoder.load(cache, phrases, phraseIds)
    }

    /** Loads only the first [count] category ids and the first [count] phrase ids. */
    fun loadFirst(cache: Rs2Cache, count: Int) {
        categoryDecoder.loadFirst(cache, categories, count)
        phraseDecoder.loadFirst(cache, phrases, count)
    }

    /** Loads only the last [count] category ids and the last [count] phrase ids. */
    fun loadLast(cache: Rs2Cache, count: Int) {
        categoryDecoder.loadLast(cache, categories, count)
        phraseDecoder.loadLast(cache, phrases, count)
    }

    fun categoryCount(cache: Rs2Cache): Int = categoryDecoder.count(cache)

    fun phraseCount(cache: Rs2Cache): Int = phraseDecoder.count(cache)

    fun tree(): String = StringBuilder().also { printTree(it) }.toString()

    /** Prints every root category (one with no parent in [categories]) as a tree, subcategories then phrases, ordered by priority. */
    fun printTree(out: Appendable = System.out) {
        val referenced = categories.values.flatMap { it.subCategories.map { ref -> ref.id } }.toSet()
        val roots = categories.keys.filter { it !in referenced }.sorted()

        roots.forEachIndexed { index, id ->
            printCategory(id, "", index == roots.lastIndex, out, mutableSetOf())
        }
    }

    private fun printCategory(id: Int, prefix: String, isLast: Boolean, out: Appendable, visited: MutableSet<Int>) {
        val category = categories[id]
        out.append(prefix).append(if (isLast) "└── " else "├── ")
            .append("[$id] ").append(category?.description ?: "?").append('\n')

        if (category == null || !visited.add(id)) return

        val childPrefix = prefix + if (isLast) "    " else "│   "
        val subCategories = category.subCategories.sortedBy { it.priority }
        val phraseRefs = category.phrases.sortedBy { it.priority }
        val total = subCategories.size + phraseRefs.size
        var index = 0

        for (ref in subCategories) {
            index++
            printCategory(ref.id, childPrefix, index == total, out, visited)
        }
        for (ref in phraseRefs) {
            index++
            val text = phrases[ref.id]?.template ?: "?"
            out.append(childPrefix).append(if (index == total) "└── " else "├── ")
                .append('"').append(text).append("\" [").append(ref.id.toString()).append(']').append('\n')
        }
    }

}
