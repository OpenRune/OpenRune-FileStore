package dev.openrune.definition.type

import dev.openrune.definition.Definition
import dev.openrune.definition.serialization.Rscm
import dev.openrune.definition.util.Type
import kotlinx.serialization.Serializable

@Serializable
data class ParamType(
    override var id: Rscm = -1,
    var type: Type? = null,
    var isMembers: Boolean = true,
    var defaultInt: Int = 0,
    var defaultString: String? = null,

) : Definition