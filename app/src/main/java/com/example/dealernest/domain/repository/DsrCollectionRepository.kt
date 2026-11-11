package com.example.dealernest.domain.repository

import com.example.dealernest.core.network.NetworkResult
import com.example.dealernest.core.network.PagedData
import com.example.dealernest.domain.model.DsrCollection

data class DsrCollectionFilters(
    val vendorId: Int? = null,
    val dateFrom: String? = null,
    val dateTo: String? = null,
)

interface DsrCollectionRepository {
    suspend fun list(filters: DsrCollectionFilters, page: Int, perPage: Int): NetworkResult<PagedData<DsrCollection>>

    // payment_method_id is hardcoded inside the impl, never a parameter here - see
    // .claude/docs/api-contract.md.
    suspend fun create(vendorId: Int, amount: Double, collectionDate: String, note: String?): NetworkResult<DsrCollection>

    suspend fun delete(id: Int): NetworkResult<Unit>
}
