package entity

import com.fasterxml.jackson.annotation.JsonIgnore

data class ApiResult(
    val data: Any,
    @JsonIgnore val error: Any? = null
) {
    constructor() : this(Any())
}
