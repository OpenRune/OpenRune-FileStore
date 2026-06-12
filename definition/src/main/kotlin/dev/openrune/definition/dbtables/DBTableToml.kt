package dev.openrune.definition.dbtables

import dev.openrune.definition.constants.ConstantProvider
import dev.openrune.definition.type.DBColumnType
import dev.openrune.definition.util.CacheVarLiteral
import dev.openrune.toml.model.TomlValue
import dev.openrune.toml.serialization.from
import dev.openrune.toml.util.InternalAPI
import java.io.File

/**
 * Loads [DBTable] definitions from TOML for [dev.openrune.cache.tools.tasks.impl.PackDBTables].
 *
 * One `.toml` per table:
 *
 * ```
 * [dbtable]
 * id = "dbtable.player_titles"
 * server_only = false
 * inherit = ""
 *
 * [dbtable.col]
 * title = "STRING"
 * title_female = "STRING"
 * requirement_stats = ["STAT", "INT", "STAT", "INT"]
 *
 * [[row]]
 * id = "dbrow.title_merchant"
 * title = "<col=c86400>Merchant</col> <name>"
 * unlock_bit = "objects.pank"
 * ```
 *
 * `[dbtable].id` and `[[row]].id` are required (integer or RSCM string).
 * Column ids auto-increment 0, 1, 2, … in `[dbtable.col]` declaration order.
 * Legacy `[[column]]` tables are still accepted.
 */
@OptIn(InternalAPI::class)
object DBTableToml {

    private const val TABLE_SECTION = "dbtable"
    private const val ROW_ARRAY = "row"

    private val reservedRowKeys = setOf("id", "name", "inherit")

    fun load(file: File): DBTable = parse(file.readText(), file.path)

    fun loadDirectory(directory: File): List<DBTable> {
        require(directory.isDirectory) { "DBTableToml: not a directory: ${directory.absolutePath}" }
        return directory.listFiles { f -> f.isFile && f.extension.equals("toml", ignoreCase = true) }
            ?.sortedBy { it.name }
            ?.map { load(it) }
            .orEmpty()
    }

    fun loadPath(path: File): List<DBTable> = when {
        path.isDirectory -> loadDirectory(path)
        path.isFile -> listOf(load(path))
        else -> error("DBTableToml: path does not exist: ${path.absolutePath}")
    }

    fun write(table: DBTable): String = DBTableTomlWriter.write(table)

    fun write(table: DBTable, file: File) = DBTableTomlWriter.write(table, file)

    fun parse(text: String, source: String = "<input>"): DBTable {
        val root = TomlValue.from(text).properties
        val tableSection = requireMap(
            root[TABLE_SECTION] ?: error("$source: missing [dbtable] section"),
            "[dbtable]",
        )

        val (tableId, tableRscmName) = resolveEntityId(
            idValue = tableSection["id"] ?: error("$source [dbtable] requires id"),
            lookupPrefixes = listOf("dbtable", "dbtables", "tables", "table"),
            context = "$source [dbtable]",
        )

        val serverOnly = optionalBool(tableSection["server_only"]) ?: false
        val inherit = optionalString(tableSection["inherit"])?.takeIf { it.isNotEmpty() }

        val columns = parseColumns(tableSection, root["column"], source)
        val columnIdByName = columns.mapNotNull { (id, col) -> col.rscmName?.let { it to id } }.toMap()
        val rows = parseRows(root[ROW_ARRAY], columns, columnIdByName, source)

        return DBTable(tableId, tableRscmName, columns, rows, serverOnly, inherit)
    }

    private fun parseRows(
        value: TomlValue?,
        columns: Map<Int, DBColumnType>,
        columnIdByName: Map<String, Int>,
        source: String,
    ): List<DBRow> {
        val entries = optionalTableArray(value, "[[row]]")
        return entries.mapIndexed { index, entry ->
            val (rowId, rowRscmName) = resolveEntityId(
                idValue = entry["id"] ?: error("$source [[row]] #$index requires id"),
                lookupPrefixes = listOf("dbrow", "dbrows", "rows", "row"),
                context = "$source [[row]] #$index",
            )
            val rowColumns = LinkedHashMap<Int, Array<Any>>()
            for ((key, cellValue) in entry) {
                if (key in reservedRowKeys) continue
                val colId = resolveColumnId(key, columns, columnIdByName, "$source [[row]] #$index")
                val colTypes = columns[colId]!!.types
                rowColumns[colId] = parseCellValues(cellValue, colTypes)
            }
            if (rowRscmName != null) rowNames[rowId] = rowRscmName
            DBRow(rowId, rowRscmName, rowColumns)
        }
    }

    private fun resolveEntityId(
        idValue: TomlValue,
        lookupPrefixes: List<String>,
        context: String,
    ): Pair<Int, String?> = when (idValue) {
        is TomlValue.Integer -> idValue.value.toInt() to null
        is TomlValue.String -> {
            val key = idValue.value
            require(key.isNotEmpty()) { "$context: id must not be empty" }
            val id = ConstantProvider.getMapping(key)
            val name = key.substringAfter('.').ifEmpty { null }
            id to name
        }
        else -> error("$context: id must be an integer or RSCM string")
    }

    private fun parseColumns(
        tableSection: Map<String, TomlValue>,
        legacyColumnArray: TomlValue?,
        source: String,
    ): Map<Int, DBColumnType> {
        val colTable = tableSection["col"] as? TomlValue.Map
        if (colTable != null) {
            return parseColTable(colTable.properties, source)
        }
        return parseLegacyColumns(legacyColumnArray, source)
    }

    /** `[dbtable.col]` — each key is the column name, value is a type string or type array. */
    private fun parseColTable(entries: Map<String, TomlValue>, source: String): Map<Int, DBColumnType> {
        val columns = LinkedHashMap<Int, DBColumnType>()
        entries.entries.forEachIndexed { index, (name, typeValue) ->
            val types = parseColumnTypeValue(typeValue, "$source [dbtable.col].$name")
            columnNames[index] = name
            columns[index] = DBColumnType(types, null, name)
        }
        return columns
    }

    private fun parseColumnTypeValue(value: TomlValue, context: String): Array<CacheVarLiteral> =
        when (value) {
            is TomlValue.String -> arrayOf(CacheVarLiteral.byName(value.value))
            is TomlValue.List -> {
                val names = requireStringList(value, context)
                Array(names.size) { i -> CacheVarLiteral.byName(names[i]) }
            }
            else -> error("$context: expected a type string or array of type strings")
        }

    private fun parseLegacyColumns(value: TomlValue?, source: String): Map<Int, DBColumnType> {
        val entries = optionalTableArray(value, "[[column]]")
        val columns = LinkedHashMap<Int, DBColumnType>()
        entries.forEachIndexed { index, entry ->
            val colId = optionalInt(entry["id"]) ?: index
            val name = requireString(entry, "name", "$source [[column]] #$index")
            val types = parseColumnTypes(entry, "$source [[column]] #$index")
            val defaults = optionalAnyArray(entry["defaults"], types)
            if (defaults != null) {
                require(defaults.size == types.size) {
                    "$source [[column]] #$index: defaults size (${defaults.size}) must match types size (${types.size})"
                }
            }
            columnNames[colId] = name
            columns[colId] = DBColumnType(types, defaults, name)
        }
        return columns
    }

    private fun parseColumnTypes(entry: Map<String, TomlValue>, context: String): Array<CacheVarLiteral> {
        entry["types"]?.let { typesValue ->
            val names = requireStringList(typesValue, "$context.types")
            return Array(names.size) { i -> CacheVarLiteral.byName(names[i]) }
        }

        val typeName = requireString(entry, "type", "$context.type")
        val type = CacheVarLiteral.byName(typeName)
        val count = optionalInt(entry["count"]) ?: 1
        require(count > 0) { "$context: count must be positive" }
        return Array(count) { type }
    }

    private fun resolveColumnId(
        key: String,
        columns: Map<Int, DBColumnType>,
        columnIdByName: Map<String, Int>,
        context: String,
    ): Int {
        columnIdByName[key]?.let { return it }
        error("$context: unknown column '$key' (define it in [dbtable.col] or [[column]])")
    }

    private fun parseCellValues(value: TomlValue, types: Array<CacheVarLiteral>): Array<Any> = when (value) {
        is TomlValue.List -> flattenList(value, types)
        else -> arrayOf(parseCellValue(value, types.firstOrNull() ?: CacheVarLiteral.INT))
    }

    private fun flattenList(value: TomlValue.List, types: Array<CacheVarLiteral>): Array<Any> {
        if (value.elements.isEmpty()) return emptyArray()
        if (value.elements.all { it is TomlValue.List }) {
            return value.elements.flatMapIndexed { pairIndex, inner ->
                (inner as TomlValue.List).elements.mapIndexed { i, element ->
                    val typeIndex = pairIndex * 2 + i
                    val type = types.getOrNull(typeIndex) ?: types.lastOrNull() ?: CacheVarLiteral.INT
                    parseCellValue(element, type)
                }
            }.toTypedArray()
        }
        return value.elements.mapIndexed { index, element ->
            val type = types.getOrNull(index) ?: types.lastOrNull() ?: CacheVarLiteral.INT
            parseCellValue(element, type)
        }.toTypedArray()
    }

    private fun parseCellValue(value: TomlValue, type: CacheVarLiteral): Any = when (value) {
        is TomlValue.Integer -> value.value.toInt()
        is TomlValue.Bool -> value.value
        is TomlValue.String -> parseStringCell(value.value, type)
        is TomlValue.List -> flattenList(value, arrayOf(type)).let { flattened ->
            require(flattened.size == 1) { "Nested list cell must flatten to a single value here" }
            flattened.single()
        }
        else -> error("Unsupported row cell value: ${value::class.simpleName}")
    }

    private fun parseStringCell(raw: String, type: CacheVarLiteral): Any {
        if (type.name == "INT" || type.name == "BOOLEAN") {
            raw.toIntOrNull()?.let { return if (type.name == "BOOLEAN") it != 0 else it }
        }
        return DBTableCellCodec.decodeString(raw, type)
    }

    private fun optionalTableArray(value: TomlValue?, label: String): List<Map<String, TomlValue>> =
        when (value) {
            null -> emptyList()
            is TomlValue.List -> value.elements.map { requireMap(it, "table array entry") }
            else -> error("Expected $label array, got ${value::class.simpleName}")
        }

    private fun requireMap(value: TomlValue, context: String): Map<String, TomlValue> =
        when (value) {
            is TomlValue.Map -> value.properties
            else -> error("$context: expected table, got ${value::class.simpleName}")
        }

    private fun requireString(map: Map<String, TomlValue>, key: String, context: String): String =
        optionalString(map[key]) ?: error("$context: missing or invalid '$key'")

    private fun requireStringList(value: TomlValue, context: String): List<String> =
        when (value) {
            is TomlValue.List -> value.elements.map {
                require(it is TomlValue.String) { "$context: expected string list" }
                it.value
            }
            is TomlValue.String -> listOf(value.value)
            else -> error("$context: expected string or string array")
        }

    private fun optionalInt(value: TomlValue?): Int? = when (value) {
        is TomlValue.Integer -> value.value.toInt()
        else -> null
    }

    private fun optionalBool(value: TomlValue?): Boolean? = when (value) {
        is TomlValue.Bool -> value.value
        else -> null
    }

    private fun optionalString(value: TomlValue?): String? = when (value) {
        is TomlValue.String -> value.value
        else -> null
    }

    private fun optionalAnyArray(value: TomlValue?, types: Array<CacheVarLiteral>): Array<Any>? =
        when (value) {
            null -> null
            is TomlValue.List -> flattenList(value, types)
            else -> error("defaults must be an array")
        }
}
