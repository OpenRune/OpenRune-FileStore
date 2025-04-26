package dev.openrune.cache.tools.dbtables

class fsw_info_fresh_table(row: FullDBRow) : DatabaseRow(row) {
    val info = multiColumn(0, StringType, StringType, StringType)
    override fun toString(): String {
        return "fsw_info_fresh_table(column0=$info)"
    }
}