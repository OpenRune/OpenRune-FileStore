package dev.openrune.definition.opcode

class OpcodeList<T> {
    private val _opcodes = mutableListOf<DefinitionOpcode<T>>()
    val registeredOpcodes: List<DefinitionOpcode<T>> get() = _opcodes

    fun add(opcode: DefinitionOpcode<T>) {
        val overlap = _opcodes
            .asSequence()
            .flatMap { existing -> existing.attachedOpcodes.asSequence() }
            .toSet()
            .intersect(opcode.attachedOpcodes)
        if (overlap.isNotEmpty()) {
            error("Opcode(s) $overlap already exist in list!")
        }

        _opcodes.add(opcode)
    }


    fun addAll(newOpcodes: List<DefinitionOpcode<T>>) {
        for (opcode in newOpcodes) {
            _opcodes.add(opcode)
        }
    }
}