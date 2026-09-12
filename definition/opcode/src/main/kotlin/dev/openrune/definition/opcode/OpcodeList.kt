package dev.openrune.definition.opcode

class OpcodeList<T> {
    private val _opcodes = mutableListOf<DefinitionOpcode<T>>()
    val registeredOpcodes: List<DefinitionOpcode<T>> get() = _opcodes

    /** Opcode -> handler, rebuilt on the first lookup after a registration. */
    private var index: Map<Int, DefinitionOpcode<T>>? = null

    /** The handler registered for [opcode], or null when nothing claims it. */
    fun forOpcode(opcode: Int): DefinitionOpcode<T>? = index(opcode = opcode)

    private fun index(opcode: Int): DefinitionOpcode<T>? {
        val current = index ?: buildIndex().also { index = it }
        return current[opcode]
    }

    private fun buildIndex(): Map<Int, DefinitionOpcode<T>> {
        val built = HashMap<Int, DefinitionOpcode<T>>(_opcodes.size * 4)
        // First-wins: `addAll` does not reject overlapping registrations.
        for (entry in _opcodes) {
            for (attached in entry.attachedOpcodes) {
                built.putIfAbsent(attached, entry)
            }
        }
        return built
    }

    fun add(opcode: DefinitionOpcode<T>) {
        val existing = index ?: buildIndex().also { index = it }
        val overlap = opcode.attachedOpcodes.filterTo(LinkedHashSet()) { existing.containsKey(it) }
        if (overlap.isNotEmpty()) {
            error("Opcode(s) $overlap already exist in list!")
        }

        _opcodes.add(opcode)
        index = null
    }


    fun addAll(newOpcodes: List<DefinitionOpcode<T>>) {
        for (opcode in newOpcodes) {
            _opcodes.add(opcode)
        }
        index = null
    }
}
