package dev.openrune.cache.filestore.definition

import dev.openrune.serialization.Rscm

interface Definition {
    var id: Rscm
    val values: MutableMap<String, Any>
    var inherit : Rscm
}