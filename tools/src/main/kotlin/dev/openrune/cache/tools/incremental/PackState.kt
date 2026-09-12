package dev.openrune.cache.tools.incremental

import com.github.michaelbull.logging.InlineLogger
import java.io.File
import java.sql.Connection
import java.sql.DriverManager

internal class PackState private constructor(
    private val connection: Connection,
    val file: File,
) : AutoCloseable {
    fun beginBuild() {
        connection.autoCommit = false
    }

    fun commit() {
        connection.commit()
    }

    fun meta(key: String): String? =
        connection.prepareStatement("SELECT value FROM meta WHERE key = ?").use { statement ->
            statement.setString(1, key)
            statement.executeQuery().use { rows -> if (rows.next()) rows.getString(1) else null }
        }

    fun putMeta(key: String, value: String) {
        connection.prepareStatement("INSERT INTO meta(key, value) VALUES(?, ?) ON CONFLICT(key) DO UPDATE SET value = excluded.value")
            .use { statement ->
                statement.setString(1, key)
                statement.setString(2, value)
                statement.executeUpdate()
            }
    }

    fun clearUnits() {
        connection.createStatement().use { it.executeUpdate("DELETE FROM unit") }
    }

    fun loadTask(task: String): Map<String, StoredUnit> {
        val units = LinkedHashMap<String, MutableUnit>()
        val byRowId = HashMap<Long, MutableUnit>()

        connection.prepareStatement("SELECT id, unit_key, hash FROM unit WHERE task = ?").use { statement ->
            statement.setString(1, task)
            statement.executeQuery().use { rows ->
                while (rows.next()) {
                    val unit = MutableUnit(rows.getLong(1), rows.getString(3))
                    units[rows.getString(2)] = unit
                    byRowId[unit.rowId] = unit
                }
            }
        }
        if (byRowId.isEmpty()) return emptyMap()

        query("SELECT unit_id, idx, archive, file, crc FROM output WHERE unit_id IN (SELECT id FROM unit WHERE task = ?)", task) { rows ->
            byRowId[rows.getLong(1)]?.outputs?.put(CacheTarget(rows.getInt(2), rows.getInt(3), rows.getInt(4)), rows.getInt(5))
        }
        query("SELECT unit_id, key, value, has_value FROM dep_constant WHERE unit_id IN (SELECT id FROM unit WHERE task = ?)", task) { rows ->
            val value = if (rows.getInt(4) == 1) rows.getInt(3) else null
            byRowId[rows.getLong(1)]?.constants?.put(rows.getString(2), value)
        }
        query("SELECT unit_id, idx, archive, file FROM dep_cache WHERE unit_id IN (SELECT id FROM unit WHERE task = ?)", task) { rows ->
            byRowId[rows.getLong(1)]?.cacheReads?.add(CacheTarget(rows.getInt(2), rows.getInt(3), rows.getInt(4)))
        }
        query("SELECT unit_id, path, hash FROM dep_file WHERE unit_id IN (SELECT id FROM unit WHERE task = ?)", task) { rows ->
            byRowId[rows.getLong(1)]?.extraFiles?.put(rows.getString(2), rows.getString(3))
        }
        query("SELECT unit_id, grp, kind, name, gv_id, sub_id FROM gameval_emit WHERE unit_id IN (SELECT id FROM unit WHERE task = ?)", task) { rows ->
            byRowId[rows.getLong(1)]?.gameVals?.add(
                GameValEmit(rows.getString(2), rows.getString(3), rows.getString(4), rows.getInt(5), rows.getInt(6))
            )
        }

        return units.mapValues { (_, unit) -> unit.freeze() }
    }

    private inline fun query(sql: String, task: String, consume: (java.sql.ResultSet) -> Unit) {
        connection.prepareStatement(sql).use { statement ->
            statement.setString(1, task)
            statement.executeQuery().use { rows -> while (rows.next()) consume(rows) }
        }
    }

    fun deleteUnits(task: String, keys: Collection<String>) {
        if (keys.isEmpty()) return
        connection.prepareStatement("DELETE FROM unit WHERE task = ? AND unit_key = ?").use { statement ->
            keys.forEach { key ->
                statement.setString(1, task)
                statement.setString(2, key)
                statement.addBatch()
            }
            statement.executeBatch()
        }
    }

    fun saveUnit(task: String, key: String, hash: String, record: UnitRecord, extraFiles: Map<String, String>) {
        connection.prepareStatement("INSERT INTO unit(task, unit_key, hash) VALUES(?, ?, ?) ON CONFLICT(task, unit_key) DO UPDATE SET hash = excluded.hash")
            .use { statement ->
                statement.setString(1, task)
                statement.setString(2, key)
                statement.setString(3, hash)
                statement.executeUpdate()
            }

        val rowId = connection.prepareStatement("SELECT id FROM unit WHERE task = ? AND unit_key = ?").use { statement ->
            statement.setString(1, task)
            statement.setString(2, key)
            statement.executeQuery().use { rows -> if (rows.next()) rows.getLong(1) else error("unit row vanished") }
        }

        listOf("output", "dep_constant", "dep_cache", "dep_file", "gameval_emit").forEach { table ->
            connection.prepareStatement("DELETE FROM $table WHERE unit_id = ?").use { statement ->
                statement.setLong(1, rowId)
                statement.executeUpdate()
            }
        }

        connection.prepareStatement("INSERT OR REPLACE INTO output(unit_id, idx, archive, file, crc) VALUES(?, ?, ?, ?, ?)").use { statement ->
            record.outputs.forEach { (target, crc) ->
                statement.setLong(1, rowId)
                statement.setInt(2, target.index)
                statement.setInt(3, target.archive)
                statement.setInt(4, target.file)
                statement.setInt(5, crc)
                statement.addBatch()
            }
            statement.executeBatch()
        }

        connection.prepareStatement("INSERT OR REPLACE INTO dep_constant(unit_id, key, value, has_value) VALUES(?, ?, ?, ?)").use { statement ->
            record.constants.forEach { (constantKey, value) ->
                statement.setLong(1, rowId)
                statement.setString(2, constantKey)
                statement.setInt(3, value ?: 0)
                statement.setInt(4, if (value == null) 0 else 1)
                statement.addBatch()
            }
            statement.executeBatch()
        }

        connection.prepareStatement("INSERT OR REPLACE INTO dep_cache(unit_id, idx, archive, file) VALUES(?, ?, ?, ?)").use { statement ->
            record.cacheReads.forEach { target ->
                statement.setLong(1, rowId)
                statement.setInt(2, target.index)
                statement.setInt(3, target.archive)
                statement.setInt(4, target.file)
                statement.addBatch()
            }
            statement.executeBatch()
        }

        connection.prepareStatement("INSERT OR REPLACE INTO dep_file(unit_id, path, hash) VALUES(?, ?, ?)").use { statement ->
            extraFiles.forEach { (path, hash) ->
                statement.setLong(1, rowId)
                statement.setString(2, path)
                statement.setString(3, hash)
                statement.addBatch()
            }
            statement.executeBatch()
        }

        connection.prepareStatement("INSERT OR REPLACE INTO gameval_emit(unit_id, grp, kind, name, gv_id, sub_id) VALUES(?, ?, ?, ?, ?, ?)").use { statement ->
            record.gameVals.forEach { emit ->
                statement.setLong(1, rowId)
                statement.setString(2, emit.group)
                statement.setString(3, emit.kind)
                statement.setString(4, emit.name)
                statement.setInt(5, emit.id)
                statement.setInt(6, emit.subId)
                statement.addBatch()
            }
            statement.executeBatch()
        }
    }

    override fun close() {
        runCatching { connection.close() }
    }

    private class MutableUnit(val rowId: Long, val hash: String) {
        val outputs = LinkedHashMap<CacheTarget, Int>()
        val constants = LinkedHashMap<String, Int?>()
        val cacheReads = LinkedHashSet<CacheTarget>()
        val extraFiles = LinkedHashMap<String, String>()
        val gameVals = LinkedHashSet<GameValEmit>()

        fun freeze() = StoredUnit(rowId, hash, outputs, constants, cacheReads, gameVals.toList(), extraFiles)
    }

    companion object {
        const val SCHEMA_VERSION = "1"

        private val logger = InlineLogger()

        fun open(file: File): PackState? {
            return try {
                Class.forName("org.sqlite.JDBC")
                file.parentFile?.mkdirs()
                val connection = DriverManager.getConnection("jdbc:sqlite:${file.absolutePath}")
                connection.createStatement().use { statement ->
                    statement.executeUpdate("PRAGMA journal_mode=WAL")
                    statement.executeUpdate("PRAGMA synchronous=NORMAL")
                    statement.executeUpdate("PRAGMA foreign_keys=ON")
                    SCHEMA.forEach(statement::executeUpdate)
                }
                PackState(connection, file)
            } catch (ex: Exception) {
                logger.warn(ex) { "Incremental packing disabled: could not open state database at $file" }
                null
            }
        }

        private val SCHEMA = listOf(
            "CREATE TABLE IF NOT EXISTS meta (key TEXT PRIMARY KEY, value TEXT NOT NULL)",
            """CREATE TABLE IF NOT EXISTS unit (
                   id INTEGER PRIMARY KEY AUTOINCREMENT,
                   task TEXT NOT NULL,
                   unit_key TEXT NOT NULL,
                   hash TEXT NOT NULL,
                   UNIQUE(task, unit_key)
               )""",
            """CREATE TABLE IF NOT EXISTS output (
                   unit_id INTEGER NOT NULL REFERENCES unit(id) ON DELETE CASCADE,
                   idx INTEGER NOT NULL, archive INTEGER NOT NULL, file INTEGER NOT NULL,
                   crc INTEGER NOT NULL,
                   PRIMARY KEY (unit_id, idx, archive, file)
               )""",
            "CREATE INDEX IF NOT EXISTS output_target ON output(idx, archive, file)",
            """CREATE TABLE IF NOT EXISTS dep_constant (
                   unit_id INTEGER NOT NULL REFERENCES unit(id) ON DELETE CASCADE,
                   key TEXT NOT NULL, value INTEGER NOT NULL, has_value INTEGER NOT NULL,
                   PRIMARY KEY (unit_id, key)
               )""",
            "CREATE INDEX IF NOT EXISTS dep_constant_key ON dep_constant(key)",
            """CREATE TABLE IF NOT EXISTS dep_cache (
                   unit_id INTEGER NOT NULL REFERENCES unit(id) ON DELETE CASCADE,
                   idx INTEGER NOT NULL, archive INTEGER NOT NULL, file INTEGER NOT NULL,
                   PRIMARY KEY (unit_id, idx, archive, file)
               )""",
            "CREATE INDEX IF NOT EXISTS dep_cache_target ON dep_cache(idx, archive, file)",
            """CREATE TABLE IF NOT EXISTS dep_file (
                   unit_id INTEGER NOT NULL REFERENCES unit(id) ON DELETE CASCADE,
                   path TEXT NOT NULL, hash TEXT NOT NULL,
                   PRIMARY KEY (unit_id, path)
               )""",
            """CREATE TABLE IF NOT EXISTS gameval_emit (
                   unit_id INTEGER NOT NULL REFERENCES unit(id) ON DELETE CASCADE,
                   grp TEXT NOT NULL, kind TEXT NOT NULL, name TEXT NOT NULL,
                   gv_id INTEGER NOT NULL, sub_id INTEGER NOT NULL,
                   PRIMARY KEY (unit_id, grp, name, gv_id, sub_id)
               )""",
        )
    }
}
