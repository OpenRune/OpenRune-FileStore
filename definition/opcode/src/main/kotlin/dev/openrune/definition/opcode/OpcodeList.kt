package dev.openrune.definition.opcode

class OpcodeList<T> {
    private val _opcodes = mutableListOf<DefinitionOpcode<T>>()
    val registeredOpcodes: List<DefinitionOpcode<T>> get() = _opcodes

    fun add(opcode: DefinitionOpcode<T>) {
        if (_opcodes.any { it.attachedOpcodes == opcode.attachedOpcodes }) {
            error("Opcode ${opcode.attachedOpcodes} already exists in list!")
        }

        _opcodes.add(opcode)
    }


    fun addAll(newOpcodes: List<DefinitionOpcode<T>>) {
        for (opcode in newOpcodes) {
            _opcodes.add(opcode)
        }
    }
}