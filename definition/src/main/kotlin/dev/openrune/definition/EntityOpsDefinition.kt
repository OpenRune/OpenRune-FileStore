package dev.openrune.definition

class EntityOpsDefinition {

    val ops = mutableListOf<Op?>()
    val subOps = mutableListOf<MutableList<SubOp>>()
    val conditionalOps = mutableListOf<MutableList<ConditionalOp>>()
    val conditionalSubOps = mutableListOf<MutableMap<Int, MutableList<ConditionalSubOp>>>()

    fun op(index: Int, text: String) = apply {
        ops.ensureSize(index) { null }
        ops[index] = Op(text)
    }

    fun setOp(index: Int, text: String) {
        ops.ensureSize(index) { null }
        ops[index] = Op(text)
    }

    fun subOp(index: Int, subID: Int, text: String) = apply {
        subOps.ensureSize(index) { mutableListOf() }
        subOps[index] += SubOp(text, subID)
    }

    fun setSubOp(index: Int, subID: Int, text: String) {
        subOps.ensureSize(index) { mutableListOf() }
        val list = subOps[index]
        list.removeIf { it.subID == subID }
        list += SubOp(text, subID)
    }

    fun conditionalOp(
        index: Int,
        text: String,
        varpID: Int,
        varbitID: Int,
        min: Int,
        max: Int
    ) = apply {
        conditionalOps.ensureSize(index) { mutableListOf() }
        conditionalOps[index] += ConditionalOp(text, varpID, varbitID, min, max)
    }

    fun setConditionalOp(
        index: Int,
        text: String,
        varpID: Int,
        varbitID: Int,
        min: Int,
        max: Int
    ) {
        conditionalOps.ensureSize(index) { mutableListOf() }
        conditionalOps[index] += ConditionalOp(text, varpID, varbitID, min, max)
    }

    fun conditionalSubOp(
        index: Int,
        subID: Int,
        text: String,
        varpID: Int,
        varbitID: Int,
        min: Int,
        max: Int
    ) = apply {
        conditionalSubOps.ensureSize(index) { mutableMapOf() }

        val map = conditionalSubOps[index]
        val list = map.getOrPut(subID) { mutableListOf() }

        list += ConditionalSubOp(text, subID, varpID, varbitID, min, max)
    }

    fun setConditionalSubOp(
        index: Int,
        subID: Int,
        text: String,
        varpID: Int,
        varbitID: Int,
        min: Int,
        max: Int
    ) {
        conditionalSubOps.ensureSize(index) { mutableMapOf() }
        val map = conditionalSubOps[index]
        val list = map.getOrPut(subID) { mutableListOf() }
        list += ConditionalSubOp(text, subID, varpID, varbitID, min, max)
    }

    /* ---------- Models ---------- */

    data class Op(val text: String)

    data class SubOp(
        val text: String,
        val subID: Int
    )

    data class ConditionalOp(
        val text: String,
        val varpID: Int,
        val varbitID: Int,
        val minValue: Int,
        val maxValue: Int
    )

    data class ConditionalSubOp(
        val text: String,
        val subID: Int,
        val varpID: Int,
        val varbitID: Int,
        val minValue: Int,
        val maxValue: Int
    )

    override fun toString(): String {
        val opsPart = ops.mapIndexedNotNull { index, op -> op?.let { "$index=${it.text}" } }
        val subOpsPart = subOps.mapIndexedNotNull { index, entries ->
            if (entries.isEmpty()) null
            else "$index=${entries.joinToString(prefix = "[", postfix = "]") { "${it.subID}:${it.text}" }}"
        }
        val conditionalOpsPart = conditionalOps.mapIndexedNotNull { index, entries ->
            if (entries.isEmpty()) null
            else "$index=${
                entries.joinToString(prefix = "[", postfix = "]") {
                    "${it.text}(varp=${it.varpID},varbit=${it.varbitID},min=${it.minValue},max=${it.maxValue})"
                }
            }"
        }
        val conditionalSubOpsPart = conditionalSubOps.mapIndexedNotNull { index, map ->
            if (map.isEmpty()) null
            else "$index=${
                map.toSortedMap().entries.joinToString(prefix = "{", postfix = "}") { (subId, entries) ->
                    "$subId:${
                        entries.joinToString(prefix = "[", postfix = "]") {
                            "${it.text}(varp=${it.varpID},varbit=${it.varbitID},min=${it.minValue},max=${it.maxValue})"
                        }
                    }"
                }
            }"
        }

        return buildString {
            append("EntityOpsDefinition(")
            append("ops=").append(opsPart)
            append(", subOps=").append(subOpsPart)
            append(", conditionalOps=").append(conditionalOpsPart)
            append(", conditionalSubOps=").append(conditionalSubOpsPart)
            append(')')
        }
    }
}

private inline fun <T> MutableList<T>.ensureSize(index: Int, default: () -> T) {
    while (size <= index) add(default())
}