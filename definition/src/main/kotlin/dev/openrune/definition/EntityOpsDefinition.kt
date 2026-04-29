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

    fun getOpOrNull(index: Int): String? = ops.getOrNull(index)?.text

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

    fun getSubOpsOrEmpty(index: Int): List<SubOp> = subOps.getOrNull(index).orEmpty()

    fun getSubOpOrNull(index: Int, subID: Int): SubOp? =
        subOps.getOrNull(index)?.firstOrNull { it.subID == subID }

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

    fun getConditionalOpsOrEmpty(index: Int): List<ConditionalOp> = conditionalOps.getOrNull(index).orEmpty()

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

    fun getConditionalSubOpsOrEmpty(index: Int): Map<Int, List<ConditionalSubOp>> =
        conditionalSubOps.getOrNull(index).orEmpty()

    fun getConditionalSubOpsBySubIdOrEmpty(index: Int, subID: Int): List<ConditionalSubOp> =
        conditionalSubOps.getOrNull(index)?.get(subID).orEmpty()

    /**
     * Structural equality for merge/patch logic. Reference equality is not enough: decoded TOML
     * often allocates a fresh empty [EntityOpsDefinition] that must compare equal to another empty instance.
     */
    fun contentEquals(other: EntityOpsDefinition): Boolean {
        if (!opsContentEquals(ops, other.ops)) return false
        if (!subOpsContentEquals(subOps, other.subOps)) return false
        if (!conditionalOpsContentEquals(conditionalOps, other.conditionalOps)) return false
        if (!conditionalSubOpsContentEquals(conditionalSubOps, other.conditionalSubOps)) return false
        return true
    }

    private fun opsContentEquals(a: List<Op?>, b: List<Op?>): Boolean {
        val n = maxOf(a.size, b.size)
        for (i in 0 until n) {
            if (a.getOrNull(i) != b.getOrNull(i)) return false
        }
        return true
    }

    private fun subOpsContentEquals(
        a: List<MutableList<SubOp>>,
        b: List<MutableList<SubOp>>
    ): Boolean {
        val n = maxOf(a.size, b.size)
        for (i in 0 until n) {
            val la = a.getOrNull(i)?.sortedBy { it.subID }.orEmpty()
            val lb = b.getOrNull(i)?.sortedBy { it.subID }.orEmpty()
            if (la != lb) return false
        }
        return true
    }

    private fun conditionalOpsContentEquals(
        a: List<MutableList<ConditionalOp>>,
        b: List<MutableList<ConditionalOp>>
    ): Boolean {
        val n = maxOf(a.size, b.size)
        for (i in 0 until n) {
            val la = a.getOrNull(i).orEmpty()
            val lb = b.getOrNull(i).orEmpty()
            if (la != lb) return false
        }
        return true
    }

    private fun conditionalSubOpsContentEquals(
        a: List<MutableMap<Int, MutableList<ConditionalSubOp>>>,
        b: List<MutableMap<Int, MutableList<ConditionalSubOp>>>
    ): Boolean {
        val n = maxOf(a.size, b.size)
        for (i in 0 until n) {
            val ma = a.getOrNull(i).orEmpty()
            val mb = b.getOrNull(i).orEmpty()
            if (ma.keys != mb.keys) return false
            for (k in ma.keys) {
                val la = ma[k].orEmpty()
                val lb = mb[k].orEmpty()
                if (la != lb) return false
            }
        }
        return true
    }

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