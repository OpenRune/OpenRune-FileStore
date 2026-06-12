package dev.openrune.cache.tools.dbtables

import dev.openrune.definition.constants.ConstantProvider
import dev.openrune.definition.constants.impl.RSCMProvider
import dev.openrune.definition.dbtables.DBTableToml
import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.dbtables.toToml
import dev.openrune.definition.util.CacheVarLiteral
import dev.openrune.definition.util.VarType
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File

class DBTableTomlTest {

    @BeforeEach
    fun loadMappings() {
        val mappingsDir = File(DBTableTomlTest::class.java.classLoader.getResource("mappings")!!.toURI())
        ConstantProvider.resetToDefaults()
        ConstantProvider.load(mappingsDir, RSCMProvider())
    }

    @AfterEach
    fun resetMappings() {
        ConstantProvider.resetToDefaults()
        ConstantProvider.mappings = emptyMap()
    }

    @Test
    fun `loads dbtable example from resources`() {
        val file = File(
            DBTableTomlTest::class.java.classLoader.getResource("dbtables/example.toml")!!.toURI(),
        )
        val table = DBTableToml.load(file)

        assertEquals(9001, table.tableId)
        assertFalse(table.serverOnly)

        assertEquals(2, table.columns.size)
        assertEquals(CacheVarLiteral.INT, table.columns[0]!!.types.single())
        assertEquals(CacheVarLiteral.BOOLEAN, table.columns[1]!!.types.single())

        assertEquals(2, table.rows.size)
        assertEquals(10, table.rows[0].columns[0]!![0])
        assertEquals(true, table.rows[0].columns[1]!![0])
        assertEquals(99, table.rows[1].columns[0]!![0])
        assertEquals(false, table.rows[1].columns[1]!![0])
    }

    @Test
    fun `dbtable and row sections use id directly`() {
        val table = DBTableToml.parse(
            """
            [dbtable]
            id = 65514
            server_only = true

            [dbtable.col]
            sortname = "STRING"

            [[row]]
            id = 9001
            sortname = "Animal Magnetism"
            """.trimIndent(),
        )

        assertEquals(65514, table.tableId)
        assertTrue(table.serverOnly)
        assertEquals(9001, table.rows.single().rowId)
        assertEquals("Animal Magnetism", table.rows.single().columns[0]!![0])
    }

    @Test
    fun `column ids auto increment from zero when omitted`() {
        val table = DBTableToml.parse(
            """
            [dbtable]
            id = 1
            server_only = false

            [dbtable.col]
            first = "INT"
            second = "INT"

            [[row]]
            id = 0
            first = 1
            second = 2
            """.trimIndent(),
        )

        assertEquals(setOf(0, 1), table.columns.keys)
        assertEquals(1, table.rows.single().columns[0]!![0])
        assertEquals(2, table.rows.single().columns[1]!![0])
    }

    @Test
    fun `dbtable col map supports multi type arrays`() {
        val table = DBTableToml.parse(
            """
            [dbtable]
            id = 1
            server_only = false

            [dbtable.col]
            title = "STRING"
            title_female = "STRING"
            requirement_stats = ["STAT", "INT", "STAT", "INT"]

            [[row]]
            id = 0
            title = "Hi"
            requirement_stats = [
                ["stats.woodcutting", 35],
                ["stats.ranged", 30],
            ]
            """.trimIndent(),
        )

        assertEquals(3, table.columns.size)
        assertEquals(CacheVarLiteral.STRING, table.columns[0]!!.types.single())
        assertEquals(4, table.columns[2]!!.types.size)
        assertEquals("Hi", table.rows.single().columns[0]!![0])
        assertEquals(1, table.rows.single().columns[2]!![0])
        assertEquals(35, table.rows.single().columns[2]!![1])
    }

    @Test
    fun `rejects unknown row columns`() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            DBTableToml.parse(
                """
                [dbtable]
                id = 1
                server_only = false

                [dbtable.col]
                title = "STRING"

                [[row]]
                id = 0
                missing = "x"
                """.trimIndent(),
            )
        }
        assertTrue(error.message!!.contains("unknown column 'missing'"))
        assertTrue(error.message!!.contains("title"))
    }

    @Test
    fun `rejects wrong value type for string column`() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            DBTableToml.parse(
                """
                [dbtable]
                id = 1
                server_only = false

                [dbtable.col]
                title = "STRING"

                [[row]]
                id = 0
                title = 123
                """.trimIndent(),
            )
        }
        assertTrue(error.message!!.contains("STRING column requires a string"))
    }

    @Test
    fun `rejects wrong value type for int column`() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            DBTableToml.parse(
                """
                [dbtable]
                id = 1
                server_only = false

                [dbtable.col]
                unlock_bit = "INT"

                [[row]]
                id = 0
                unlock_bit = "not-an-int"
                """.trimIndent(),
            )
        }
        assertTrue(error.message!!.contains("INT column requires an integer"))
    }

    @Test
    fun `coord column accepts packed int x z level and conventional string`() {
        fun parseCoords(value: String): Int {
            val table = DBTableToml.parse(
                """
                [dbtable]
                id = 1
                server_only = false

                [dbtable.col]
                spawn = "COORD"

                [[row]]
                id = 0
                spawn = $value
                """.trimIndent(),
            )
            return table.rows.single().columns[0]!![0] as Int
        }

        val fromArray = parseCoords("[3200, 3200, 0]")
        val fromTwo = parseCoords("[3200, 3200]")
        val fromConventional = parseCoords("\"0_50_50_32_32\"")
        assertEquals(fromArray, fromTwo)
        assertEquals(fromArray, parseCoords(fromArray.toString()))
        assertEquals(fromConventional, parseCoords(fromConventional.toString()))
    }

    @Test
    fun `coord column exports as x z array omitting level zero`() {
        val table = dbTable(1) {
            serverOnly(false)
            column("spawn", 0, VarType.COORDGRID)
            row(0) {
                column(0, 3200 shl 14 or 3200)
            }
        }
        val toml = table.toToml()
        assertTrue(toml.contains("spawn = [3200, 3200]"))
        assertFalse(toml.contains("spawn = [3200, 3200, 0]"))

        val reparsed = DBTableToml.parse(toml)
        assertEquals(table.rows.single().columns[0]!![0], reparsed.rows.single().columns[0]!![0])
    }

    @Test
    fun `string columns keep literals and rscm columns resolve refs`() {
        val table = DBTableToml.parse(
            """
            [dbtable]
            id = 1
            server_only = false

            [dbtable.col]
            title = "STRING"
            note = "STRING"
            unlock_bit = "OBJ"
            category = "OBJ"

            [[row]]
            id = 64904
            title = "<col=c86400>Merchant</col> <name>"
            note = "The title is unlocked by completing 500 successful trades."
            unlock_bit = "objects.pank"
            category = "objects.merlin_rune_portal"
            """.trimIndent(),
        )

        val row = table.rows.single()
        assertEquals("<col=c86400>Merchant</col> <name>", row.columns[0]!![0])
        assertEquals(69, row.columns[2]!![0])
        assertEquals(ConstantProvider.getMapping("objects.merlin_rune_portal"), row.columns[3]!![0])
    }

    @Test
    fun `export always includes server_only`() {
        val toml = dbTable(1) {
            serverOnly(false)
            column("value", 0, VarType.INT)
            row(0) { column(0, 1) }
        }.toToml()

        assertTrue(toml.contains("server_only = false"))
        assertTrue(toml.contains("[dbtable]"))
        assertTrue(toml.contains("[dbtable.col]"))
        assertTrue(toml.contains("[[row]]"))
    }

    @Test
    fun `export does not double prefix rscm keys`() {
        val toml = dbTable(1) {
            serverOnly(false)
            column("unlock_bit", 0, VarType.OBJ)
            row(0) {
                columnRSCM(0, "objects.pank")
            }
        }.toToml()

        assertTrue(toml.contains("unlock_bit = \"objects.pank\""))
        assertFalse(toml.contains("objects.objects."))
        assertFalse(toml.contains("obj.obj."))
    }

    @Test
    fun `dsl round trips through toml export`() {
        val original = dbTable(9001) {
            column("value", 0, VarType.INT)
            column("enabled", 1, VarType.BOOLEAN)
            row(0) {
                column(0, 10)
                column(1, true)
            }
            row(1) {
                column(0, 99)
                column(1, false)
            }
        }

        val parsed = DBTableToml.parse(original.toToml())

        assertEquals(original.tableId, parsed.tableId)
        assertEquals(original.columns.keys, parsed.columns.keys)
        assertEquals(original.rows.size, parsed.rows.size)
        assertEquals(10, parsed.rows[0].columns[0]!![0])
        assertEquals(true, parsed.rows[0].columns[1]!![0])
        assertEquals(99, parsed.rows[1].columns[0]!![0])
        assertEquals(false, parsed.rows[1].columns[1]!![0])
    }
}
