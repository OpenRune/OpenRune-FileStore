package dev.openrune.definition

class EntityOpsDefinition {

    // Allocated on first touch: every item, npc and object owns one of these and most only ever
    // use `ops`, if anything at all.
    private var _ops: MutableList<Op?>? = null
    private var _subOps: MutableList<MutableList<SubOp>>? = null
    private var _conditionalOps: MutableList<MutableList<ConditionalOp>>? = null
    private var _conditionalSubOps: MutableList<MutableMap<Int, MutableList<ConditionalSubOp>>>? = null

    val ops: MutableList<Op?>
        get() = _ops ?: ArrayList<Op?>(DEFAULT_OP_SLOTS).also { _ops = it }

    val subOps: MutableList<MutableList<SubOp>>
        get() = _subOps ?: ArrayList<MutableList<SubOp>>(DEFAULT_OP_SLOTS).also { _subOps = it }

    val conditionalOps: MutableList<MutableList<ConditionalOp>>
        get() = _conditionalOps ?: ArrayList<MutableList<ConditionalOp>>(DEFAULT_OP_SLOTS).also { _conditionalOps = it }

    val conditionalSubOps: MutableList<MutableMap<Int, MutableList<ConditionalSubOp>>>
        get() = _conditionalSubOps
            ?: ArrayList<MutableMap<Int, MutableList<ConditionalSubOp>>>(DEFAULT_OP_SLOTS).also { _conditionalSubOps = it }

    // Read-only views. Touching `ops`/`subOps`/... materialises the backing list, so read-only
    // callers should use these instead.
    val opsOrEmpty: List<Op?> get() = _ops ?: emptyList()
    val subOpsOrEmpty: List<List<SubOp>> get() = _subOps ?: emptyList()
    val conditionalOpsOrEmpty: List<List<ConditionalOp>> get() = _conditionalOps ?: emptyList()
    val conditionalSubOpsOrEmpty: List<Map<Int, List<ConditionalSubOp>>> get() = _conditionalSubOps ?: emptyList()

    /** True when nothing at all has been recorded on this instance. */
    fun isEmpty(): Boolean =
        _ops.isNullOrEmpty() && _subOps.isNullOrEmpty() &&
            _conditionalOps.isNullOrEmpty() && _conditionalSubOps.isNullOrEmpty()

    fun op(index: Int, text: String) = apply {
        ops.ensureSize(index) { null }
        ops[index] = Op.of(text)
    }

    fun setOp(index: Int, text: String) {
        ops.ensureSize(index) { null }
        ops[index] = Op.of(text)
    }

    fun getOpOrNull(index: Int): String? = _ops?.getOrNull(index)?.text

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

    fun getSubOpsOrEmpty(index: Int): List<SubOp> = _subOps?.getOrNull(index).orEmpty()

    fun getSubOpOrNull(index: Int, subID: Int): SubOp? =
        _subOps?.getOrNull(index)?.firstOrNull { it.subID == subID }

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

    fun getConditionalOpsOrEmpty(index: Int): List<ConditionalOp> = _conditionalOps?.getOrNull(index).orEmpty()

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
        _conditionalSubOps?.getOrNull(index).orEmpty()

    fun getConditionalSubOpsBySubIdOrEmpty(index: Int, subID: Int): List<ConditionalSubOp> =
        _conditionalSubOps?.getOrNull(index)?.get(subID).orEmpty()

    /**
     * Structural equality for merge/patch logic. Reference equality is not enough: decoded TOML
     * often allocates a fresh empty [EntityOpsDefinition] that must compare equal to another empty instance.
     */
    fun contentEquals(other: EntityOpsDefinition): Boolean {
        if (!opsContentEquals(_ops.orEmpty(), other._ops.orEmpty())) return false
        if (!subOpsContentEquals(_subOps.orEmpty(), other._subOps.orEmpty())) return false
        if (!conditionalOpsContentEquals(_conditionalOps.orEmpty(), other._conditionalOps.orEmpty())) return false
        if (!conditionalSubOpsContentEquals(_conditionalSubOps.orEmpty(), other._conditionalSubOps.orEmpty())) return false
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

    data class Op(val text: String) {
        companion object {
            private val pool = arrayOfNulls<Op>(512)

            /**
             * Ops are immutable and drawn from a tiny vocabulary ("Attack", "Take", ...), so equal
             * ones share an instance instead of every definition holding its own. Direct-mapped
             * and race-tolerant: a lost slot only costs the sharing, never correctness.
             */
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
        val opsPart = _ops.orEmpty().mapIndexedNotNull { index, op -> op?.let { "$index=${it.text}" } }
        val subOpsPart = _subOps.orEmpty().mapIndexedNotNull { index, entries ->
            if (entries.isEmpty()) null
            else "$index=${entries.joinToString(prefix = "[", postfix = "]") { "${it.subID}:${it.text}" }}"
        }
        val conditionalOpsPart = _conditionalOps.orEmpty().mapIndexedNotNull { index, entries ->
            if (entries.isEmpty()) null
            else "$index=${
                entries.joinToString(prefix = "[", postfix = "]") {
                    "${it.text}(varp=${it.varpID},varbit=${it.varbitID},min=${it.minValue},max=${it.maxValue})"
                }
            }"
        }
        val conditionalSubOpsPart = _conditionalSubOps.orEmpty().mapIndexedNotNull { index, map ->
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

private const val DEFAULT_OP_SLOTS = 5

private inline fun <T> MutableList<T>.ensureSize(index: Int, default: () -> T) {
    while (size <= index) add(default())
}

