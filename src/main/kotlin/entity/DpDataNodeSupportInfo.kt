package entity

import com.fasterxml.jackson.annotation.JsonIgnore
import io.vertx.core.json.JsonObject

data class DpDataNodeSupportInfo(
    val ownedPermissions: Set<String>,
    val missedPermissions: Set<String>,
    val exceptionMessages: Set<String>,
    val timeout: Boolean = false,
    val modes: Set<DpTransmissionMode>,
    val unsupportModes: Set<DpTransmissionMode>,
    @JsonIgnore val agentInfoBean: JsonObject? = null
) {
    constructor() : this(emptySet(), emptySet(), emptySet(), false, emptySet(), emptySet())
}
