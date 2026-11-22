package dev.openrune.cache.tools.cs2

/**
 * Bridges the zwyz decompiler's spelling of pre-231 ("legacy") arrays to what Neptune accepts.
 *
 * Before arrays v2 an array handed to a proc is a dummy int argument and the callee's
 * `intarray $x` parameter is just an int slot. Neptune spells that argument as the array's
 * declared name without a dollar sign, e.g. `~sort(intarray, 0, 5)` after `def_int $intarray(10)`.
 * zwyz always prints the argument as `intarray0`, the array slot, even when it declared the
 * variable as `$intarray`, and it types unknown-element arrays as `unknownarray`, which Neptune
 * does not know. Both are fixed here; dumps from 231 onward are left untouched.
 */
object LegacyArraySyntax {
    const val ARRAYS_V2_REVISION = 231

    private val BARE_REF = Regex("""(?<![$\w])([a-z]+array)(\d+)\b(?!\s*\()""")
    private val DECLARED = Regex("""def_[a-z]+\s+\$([a-z]+array\d*)\(""")
    private val PARAM = Regex("""\b[a-z]+array\s+\$([a-z]+array\d*)\b""")

    fun applies(revision: Int) = revision < ARRAYS_V2_REVISION

    fun rewrite(script: String): String {
        var text = script.replace("unknownarray", "intarray")
        val declared = (DECLARED.findAll(text).map { it.groupValues[1] } + PARAM.findAll(text).map { it.groupValues[1] })
            .toSet()
        if (declared.isEmpty()) return text
        text = BARE_REF.replace(text) { match ->
            val base = match.groupValues[1]
            val exact = base + match.groupValues[2]
            when {
                exact in declared -> exact
                base in declared -> base
                else -> declared.firstOrNull { it.startsWith(base) } ?: declared.singleOrNull() ?: exact
            }
        }
        return text
    }
}
