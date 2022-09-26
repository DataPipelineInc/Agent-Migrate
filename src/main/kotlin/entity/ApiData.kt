package entity

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.JsonNode

data class ApiData(
    val itemsPerPage: Int,
    val itemCount: Int,
    val page: Int,
    val items: Collection<JsonNode>,
    val hasMore: Boolean = false,
    @JsonIgnore val scrollId: String? = null
) {
    constructor() : this(20, 0, 0, emptyList())
}
