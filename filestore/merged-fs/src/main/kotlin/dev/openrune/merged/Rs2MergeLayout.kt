package dev.openrune.merged

object Rs2MergeLayout {

    const val ID_OFFSET = 100_000

    const val INDEX_FLOOR = 30

    enum class Promoted(val sourceIndex: Int, val bits: Int) {
        OBJ(19, 8),
        NPC(18, 7),
        SEQ(20, 7),
        ENUM(17, 8),
        STRUCT(22, 5),
    }

    enum class Grouped(val group: Int) {
        VAR_BIT(69),
        HITMARK(46),
        HEADBAR(72),
    }

    const val CONFIG_INDEX = 2

    val GROUPED_ARCHIVES: Set<Int> = Grouped.entries.mapTo(LinkedHashSet()) { it.group }
}
