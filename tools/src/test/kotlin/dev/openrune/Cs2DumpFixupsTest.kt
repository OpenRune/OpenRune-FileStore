package dev.openrune

import dev.openrune.cache.tools.cs2.Cs2DumpFixups
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * 242 started using the client's numbered string substitution slots (`bond_main_init`,
 * `[proc,script1277]`). Neptune's `Tag` fragment knows `str` but not `str0`, and `STRING_TAG`'s
 * value cannot contain `<`, so with `stringTemplates` on from 240 the `<` falls through to
 * `STRING_EXPR_START` and the parse fails. `\<` is the grammar's literal-`<` escape and compiles to
 * a bare `<`, so escaping is safe.
 */
class Cs2DumpFixupsTest {

    @TempDir
    lateinit var tempDir: File

    private fun pkg(vararg scripts: Pair<String, String>): File {
        val root = File(tempDir, "cs2").apply { mkdirs() }
        File(root, "script").mkdirs()
        File(root, ".prelude").mkdirs()
        for ((name, body) in scripts) {
            File(root, "script/$name").writeText(body)
        }
        return root
    }

    /** The prelude and commands.sym a bundle built before the 242 rename would carry. */
    private fun withPreRenameTables(root: File) {
        File(root, ".prelude/misc.cs2").writeText(
            """
            [command,seq_prefetch](seq ${'$'}x1)
            [command,getpreloadprogress]()(int)
            """.trimIndent() + "\n"
        )
        File(root, "symbols").mkdirs()
        File(root, "symbols/commands.sym").writeText("3154\tgetpreloadprogress\n3189\tseq_prefetch\n")
    }

    @Test
    fun `declares the 242 name for a renamed command`() {
        val root = pkg(
            "[clientscript,script10030].cs2" to "[clientscript,script10030]\npreload_keyframeset(xarth_death_01);\n"
        )
        withPreRenameTables(root)

        Cs2DumpFixups.apply(root)

        val prelude = File(root, ".prelude/misc.cs2").readText()
        assertEquals(true, prelude.contains("""[command,preload_keyframeset](seq ${'$'}x1)""")) { prelude }
        // The name zwyz publishes stays, so a dump from before the rename still resolves.
        assertEquals(true, prelude.contains("""[command,seq_prefetch](seq ${'$'}x1)""")) { prelude }

        // Same opcode, so the writer emits the same command either way.
        val symbols = File(root, "symbols/commands.sym").readLines()
        assertEquals(true, symbols.contains("3189\tpreload_keyframeset")) { symbols.toString() }
        assertEquals(true, symbols.contains("3189\tseq_prefetch")) { symbols.toString() }
    }

    @Test
    fun `renamed command declaration is idempotent`() {
        val root = pkg(
            "[clientscript,script10030].cs2" to "[clientscript,script10030]\npreload_keyframeset(xarth_death_01);\n"
        )
        withPreRenameTables(root)

        Cs2DumpFixups.apply(root)
        val preludeOnce = File(root, ".prelude/misc.cs2").readText()
        val symbolsOnce = File(root, "symbols/commands.sym").readText()
        Cs2DumpFixups.apply(root)

        assertEquals(preludeOnce, File(root, ".prelude/misc.cs2").readText())
        assertEquals(symbolsOnce, File(root, "symbols/commands.sym").readText())
        assertEquals(1, preludeOnce.split("[command,preload_keyframeset]").size - 1) { preludeOnce }
    }

    private fun script(root: File, name: String) = File(root, "script/$name").readText()

    @Test
    fun `escapes numbered string slots`() {
        val name = "[clientscript,bond_main_init].cs2"
        val root = pkg(
            name to """
                [clientscript,bond_main_init]
                def_string ${'$'}string0 = "<str0=<${'$'}string1>><br>You have <col=ffffff><str0>@or4@ bonds.";
            """.trimIndent()
        )

        Cs2DumpFixups.apply(root)

        val text = script(root, name)
        assertEquals(true, text.contains("""\<str0=""")) { "slot assignment should be escaped: $text" }
        assertEquals(true, text.contains("""\<str0>""")) { "slot read should be escaped: $text" }
        // The valid tag alongside them is left alone.
        assertEquals(true, text.contains("<col=ffffff>")) { "valid tag should be untouched: $text" }
    }

    @Test
    fun `leaves valid str tags alone`() {
        val name = "[clientscript,plain].cs2"
        val body = """
            [clientscript,plain]
            def_string ${'$'}s = "<str>bare</str> and <col=ff0000>red</col>";
        """.trimIndent()

        val root = pkg(name to body)
        Cs2DumpFixups.apply(root)

        assertEquals(body, script(root, name))
    }

    @Test
    fun `is idempotent across installs`() {
        val name = "[clientscript,bond_main_init].cs2"
        val root = pkg(
            name to """
                [clientscript,bond_main_init]
                def_string ${'$'}s = "<str0=<${'$'}v>><str12=x>";
            """.trimIndent()
        )

        Cs2DumpFixups.apply(root)
        val once = script(root, name)
        Cs2DumpFixups.apply(root)
        val twice = script(root, name)

        assertEquals(once, twice) { "a second install must not double-escape" }
        assertEquals(true, once.contains("""\<str0=""")) { once }
        assertEquals(true, once.contains("""\<str12=""")) { once }
        assertEquals(false, once.contains("""\\<""")) { "no double backslash: $once" }
    }
}
