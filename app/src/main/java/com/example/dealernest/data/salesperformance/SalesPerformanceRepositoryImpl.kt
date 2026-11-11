package com.example.dealernest.data.salesperformance

import com.example.dealernest.core.network.ApiService
import com.example.dealernest.core.network.NetworkResult
import com.example.dealernest.core.network.mapNetworkException
import com.example.dealernest.data.salesperformance.dto.toDomain
import com.example.dealernest.domain.model.SalesPerformance
import com.example.dealernest.domain.repository.SalesPerformanceRepository
import javax.inject.Inject

class SalesPerformanceRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
) : SalesPerformanceRepository {

    // Not routed through safeApiCall - its envelope shape (a list + system-wide meta, not a
    // single object or page cursors) doesn't match ApiEnvelope/PaginatedEnvelope. Still reuses
    // mapNetworkException for the same HttpException/IOException/SerializationException
    // handling. adminId is null for roles that see everyone, or the caller's own id for roles
    // scoped to just their own details - see RoleLevel.canSeeAllSalesPerformance().
    override suspend fun get(dateFrom: String?, dateTo: String?, adminId: Int?): NetworkResult<SalesPerformance> = try {
        val response = apiService.getSalesPerformance(dateFrom, dateTo, adminId)
        val entries = response.data
        val meta = response.meta
        if (response.success && entries != null && meta != null) {
            NetworkResult.Success(SalesPerformance(entries.map { it.toDomain() }, meta.toDomain()))
        } else {
            NetworkResult.Error(message = response.message ?: "Something went wrong")
        }
    } catch (throwable: Throwable) {
        mapNetworkException(throwable)
    }
}
