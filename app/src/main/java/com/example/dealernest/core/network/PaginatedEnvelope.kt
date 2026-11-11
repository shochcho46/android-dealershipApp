package com.example.dealernest.core.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PaginatedEnvelope<T>(
    val success: Boolean = false,
    val message: String? = null,
    val data: List<T>? = null,
    val meta: PageMeta? = null,
)

// filteredTotalAmount/totalAllTime are DSR-Collections-only fields; null for Orders-by-placed-by,
// which shares this same meta shape otherwise.
@Serializable
data class PageMeta(
    @SerialName("current_page") val currentPage: Int = 1,
    @SerialName("last_page") val lastPage: Int = 1,
    @SerialName("per_page") val perPage: Int = 15,
    val total: Int = 0,
    @SerialName("filtered_total_amount") val filteredTotalAmount: String? = null,
    @SerialName("total_all_time") val totalAllTime: String? = null,
)

data class PagedData<T>(val items: List<T>, val meta: PageMeta)
