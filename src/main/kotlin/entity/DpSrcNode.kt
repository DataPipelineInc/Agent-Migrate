package entity

import com.fasterxml.jackson.annotation.JsonIgnore
import io.vertx.core.json.JsonObject

data class DpSrcNode(
    val relationId: Int,
    var removed: Boolean = false,
    val node: DpDataNode,
    var basicConfig: DpSrcNodeBasicConfig? = null,
    @JsonIgnore val limitConfig: JsonObject? = null,
    @JsonIgnore val policyConfig: JsonObject? = null,
) {
    constructor() : this(0, false, DpDataNode())
}
