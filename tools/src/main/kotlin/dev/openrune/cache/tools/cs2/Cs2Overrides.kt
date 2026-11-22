package dev.openrune.cache.tools.cs2

import java.io.File

/**
 * Scripts and symbols a project holds outside the Neptune directory - typically the `cs2/`
 * resources of its content packs plus symbols derived from its gamevals. [NeptuneProjectManifest]
 * writes them into `neptune.toml` and `symbols_custom/` so the compiler, and any IDE reading the
 * same config, sees them without scripts being copied inside the project.
 *
 * @property sources `.cs2` files or directories of them. A script here with the same
 * `[trigger,name]` as a vanilla one replaces it.
 * @property symbols symbol lines by table (`component`, `enum`, `clientscript`, ...); they take
 * precedence over the dumped `symbols/` files on any overlapping name.
 */
data class Cs2Overrides(
    val sources: List<File> = emptyList(),
    val symbols: Map<String, List<String>> = emptyMap(),
)
