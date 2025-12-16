package dev.openrune.definition.opcode

class OpcodeList<T> {
    private val _opcodes = mutableListOf<DefinitionOpcode<T>>()
    val registeredOpcodes: List<DefinitionOpcode<T>> get() = _opcodes
    
    private val singleOpcodeMap: MutableMap<Int, DefinitionOpcode<T>> = mutableMapOf()
    private val rangeOpcodes: MutableList<DefinitionOpcode<T>> = mutableListOf()

    fun add(opcode: DefinitionOpcode<T>) {
        if (_opcodes.any { it.attachedOpcodes == opcode.attachedOpcodes }) {
            error("Opcode ${opcode.attachedOpcodes} already exists in list!")
        }

        _opcodes.add(opcode)
        
        val range = opcode.attachedOpcodes
        if (range.first == range.last) {
            singleOpcodeMap[range.first] = opcode
        } else {
            rangeOpcodes.add(opcode)
        }
    }

    fun addAll(newOpcodes: List<DefinitionOpcode<T>>) {
        for (opcode in newOpcodes) {
            add(opcode)
        }
    }
    
    fun getOpcode(opcode: Int): DefinitionOpcode<T>? {
        // Fast path: check single opcode map first (O(1))
        singleOpcodeMap[opcode]?.let { return it }
        
        // Slower path: check ranges (usually very few)
        return rangeOpcodes.firstOrNull { opcode in it.attachedOpcodes }
    }
}