package dev.openrune.definition

class EntityOpsDefinition(
    val ops: List<Op?> = emptyList(),
    val subOps: List<List<SubOp>> = emptyList(),
    val conditionalOps: List<List<ConditionalOp>> = emptyList(),
    val conditionalSubOps: List<Map<Int, List<ConditionalSubOp>>> = emptyList(),
) {

    val opsOrEmpty: List<Op?> get() = ops
    val subOpsOrEmpty: List<List<SubOp>> get() = subOps
    val conditionalOpsOrEmpty: List<List<ConditionalOp>> get() = conditionalOps
    val conditionalSubOpsOrEmpty: List<Map<Int, List<ConditionalSubOp>>> get() = conditionalSubOps

    /** True when nothing at all has been recorded on this instance. */
    fun isEmpty(): Boolean =
        ops.isEmpty() && subOps.isEmpty() && conditionalOps.isEmpty() && conditionalSubOps.isEmpty()

    fun getOpOrNull(index: Int): String? = ops.getOrNull(index)?.text

    fun getSubOpsOrEmpty(index: Int): List<SubOp> = subOps.getOrNull(index).orEmpty()

    fun getSubOpOrNull(index: Int, subID: Int): SubOp? =
        subOps.getOrNull(index)?.firstOrNull { it.subID == subID }

    fun getConditionalOpsOrEmpty(index: Int): List<ConditionalOp> = conditionalOps.getOrNull(index).orEmpty()

    fun getConditionalSubOpsOrEmpty(index: Int): Map<Int, List<ConditionalSubOp>> =
        conditionalSubOps.getOrNull(index).orEmpty()

    fun getConditionalSubOpsBySubIdOrEmpty(index: Int, subID: Int): List<ConditionalSubOp> =
        conditionalSubOps.getOrNull(index)?.get(subID).orEmpty()

    fun toBuilder(): EntityOpsBuilder = EntityOpsBuilder.from(this)

    /**
     * Structural equality for merge/patch logic. Unlike [equals], trailing empty slots are
     * ignored, so `[Open, null]` compares equal to `[Open]`.
     */
    fun contentEquals(other: EntityOpsDefinition): Boolean {
        if (!opsContentEquals(ops, other.ops)) return false
        if (!subOpsContentEquals(subOps, other.subOps)) return false
        if (!conditionalOpsContentEquals(conditionalOps, other.conditionalOps)) return false
        if (!conditionalSubOpsContentEquals(conditionalSubOps, other.conditionalSubOps)) return false
        return true
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EntityOpsDefinition) return false
        return ops == other.ops &&
            subOps == other.subOps &&
            conditionalOps == other.conditionalOps &&
            conditionalSubOps == other.conditionalSubOps
    }

    override fun hashCode(): Int {
        var result = ops.hashCode()
        result = 31 * result + subOps.hashCode()
        result = 31 * result + conditionalOps.hashCode()
        result = 31 * result + conditionalSubOps.hashCode()
        return result
    }

    private fun opsContentEquals(a: List<Op?>, b: List<Op?>): Boolean {
        val n = maxOf(a.size, b.size)
        for (i in 0 until n) {
            if (a.getOrNull(i) != b.getOrNull(i)) return false
        }
        return true
    }

    private fun subOpsContentEquals(a: List<List<SubOp>>, b: List<List<SubOp>>): Boolean {
        val n = maxOf(a.size, b.size)
        for (i in 0 until n) {
            val la = a.getOrNull(i)?.sortedBy { it.subID }.orEmpty()
            val lb = b.getOrNull(i)?.sortedBy { it.subID }.orEmpty()
            if (la != lb) return false
        }
        return true
    }

    private fun conditionalOpsContentEquals(a: List<List<ConditionalOp>>, b: List<List<ConditionalOp>>): Boolean {
        val n = maxOf(a.size, b.size)
        for (i in 0 until n) {
            if (a.getOrNull(i).orEmpty() != b.getOrNull(i).orEmpty()) return false
        }
        return true
    }

    private fun conditionalSubOpsContentEquals(
        a: List<Map<Int, List<ConditionalSubOp>>>,
        b: List<Map<Int, List<ConditionalSubOp>>>
    ): Boolean {
        val n = maxOf(a.size, b.size)
        for (i in 0 until n) {
            val ma = a.getOrNull(i).orEmpty()
            val mb = b.getOrNull(i).orEmpty()
            if (ma.keys != mb.keys) return false
            for (k in ma.keys) {
                if (ma[k].orEmpty() != mb[k].orEmpty()) return false
            }
        }
        return true
    }

    companion object {
        @JvmField
        val EMPTY = EntityOpsDefinition()
    }

    data class Op(val text: String) {
        companion object {
            private val pool = arrayOfNulls<Op>(512)

            fun of(text: String): Op {
                val slot = text.hashCode() and (pool.size - 1)
                val cached = pool[slot]
                if (cached != null && cached.text == text) return cached
                return Op(text).also { pool[slot] = it }
            }
        }
    }

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

class EntityOpsBuilder {

    private var _ops: MutableList<EntityOpsDefinition.Op?>? = null
    private var _subOps: MutableList<MutableList<EntityOpsDefinition.SubOp>>? = null
    private var _conditionalOps: MutableList<MutableList<EntityOpsDefinition.ConditionalOp>>? = null
    private var _conditionalSubOps: MutableList<MutableMap<Int, MutableList<EntityOpsDefinition.ConditionalSubOp>>>? = null

    val ops: MutableList<EntityOpsDefinition.Op?>
        get() = _ops ?: ArrayList<EntityOpsDefinition.Op?>(DEFAULT_OP_SLOTS).also { _ops = it }

    val subOps: MutableList<MutableList<EntityOpsDefinition.SubOp>>
        get() = _subOps ?: ArrayList<MutableList<EntityOpsDefinition.SubOp>>(DEFAULT_OP_SLOTS).also { _subOps = it }

    val conditionalOps: MutableList<MutableList<EntityOpsDefinition.ConditionalOp>>
        get() = _conditionalOps
            ?: ArrayList<MutableList<EntityOpsDefinition.ConditionalOp>>(DEFAULT_OP_SLOTS).also { _conditionalOps = it }

    val conditionalSubOps: MutableList<MutableMap<Int, MutableList<EntityOpsDefinition.ConditionalSubOp>>>
        get() = _conditionalSubOps
            ?: ArrayList<MutableMap<Int, MutableList<EntityOpsDefinition.ConditionalSubOp>>>(DEFAULT_OP_SLOTS)
                .also { _conditionalSubOps = it }

    fun isEmpty(): Boolean =
        _ops.isNullOrEmpty() && _subOps.isNullOrEmpty() &&
            _conditionalOps.isNullOrEmpty() && _conditionalSubOps.isNullOrEmpty()

    fun op(index: Int, text: String) = apply {
        ops.ensureSize(index) { null }
        ops[index] = EntityOpsDefinition.Op.of(text)
    }

    fun setOp(index: Int, text: String) {
        op(index, text)
    }

    fun subOp(index: Int, subID: Int, text: String) = apply {
        subOps.ensureSize(index) { mutableListOf() }
        subOps[index] += EntityOpsDefinition.SubOp(text, subID)
    }

    fun setSubOp(index: Int, subID: Int, text: String) {
        subOps.ensureSize(index) { mutableListOf() }
        val list = subOps[index]
        list.removeIf { it.subID == subID }
        list += EntityOpsDefinition.SubOp(text, subID)
    }

    fun conditionalOp(index: Int, text: String, varpID: Int, varbitID: Int, min: Int, max: Int) = apply {
        conditionalOps.ensureSize(index) { mutableListOf() }
        conditionalOps[index] += EntityOpsDefinition.ConditionalOp(text, varpID, varbitID, min, max)
    }

    fun setConditionalOp(index: Int, text: String, varpID: Int, varbitID: Int, min: Int, max: Int) {
        conditionalOp(index, text, varpID, varbitID, min, max)
    }

    fun conditionalSubOp(index: Int, subID: Int, text: String, varpID: Int, varbitID: Int, min: Int, max: Int) = apply {
        conditionalSubOps.ensureSize(index) { mutableMapOf() }
        val map = conditionalSubOps[index]
        val list = map.getOrPut(subID) { mutableListOf() }
        list += EntityOpsDefinition.ConditionalSubOp(text, subID, varpID, varbitID, min, max)
    }

    fun setConditionalSubOp(index: Int, subID: Int, text: String, varpID: Int, varbitID: Int, min: Int, max: Int) {
        conditionalSubOp(index, subID, text, varpID, varbitID, min, max)
    }

    fun include(definition: EntityOpsDefinition) = apply {
        definition.ops.forEachIndexed { index, entry ->
            if (entry != null) op(index, entry.text)
        }
        definition.subOps.forEachIndexed { index, subOps ->
            subOps.forEach { subOp(index, it.subID, it.text) }
        }
        definition.conditionalOps.forEachIndexed { index, conditionals ->
            conditionals.forEach { conditionalOp(index, it.text, it.varpID, it.varbitID, it.minValue, it.maxValue) }
        }
        definition.conditionalSubOps.forEachIndexed { index, bySub ->
            bySub.values.forEach { conditionals ->
                conditionals.forEach {
                    conditionalSubOp(index, it.subID, it.text, it.varpID, it.varbitID, it.minValue, it.maxValue)
                }
            }
        }
    }

    fun build(): EntityOpsDefinition {
        if (isEmpty()) return EntityOpsDefinition.EMPTY

        val built = EntityOpsDefinition(
            ops = _ops?.toList() ?: emptyList(),
            subOps = _subOps?.map { it.toList() } ?: emptyList(),
            conditionalOps = _conditionalOps?.map { it.toList() } ?: emptyList(),
            conditionalSubOps = _conditionalSubOps
                ?.map { bySub -> bySub.entries.associate { (subId, list) -> subId to list.toList() } }
                ?: emptyList(),
        )

        val slot = built.hashCode() and (pool.size - 1)
        val cached = pool[slot]
        if (cached != null && cached == built) return cached
        pool[slot] = built
        return built
    }

    companion object {
        private val pool = arrayOfNulls<EntityOpsDefinition>(2048)

        fun from(definition: EntityOpsDefinition): EntityOpsBuilder =
            EntityOpsBuilder().include(definition)
    }
}

private const val DEFAULT_OP_SLOTS = 5

private inline fun <T> MutableList<T>.ensureSize(index: Int, default: () -> T) {
    while (size <= index) add(default())
}
