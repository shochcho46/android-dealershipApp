package com.example.dealernest.domain.repository

import com.example.dealernest.core.network.NetworkResult
import com.example.dealernest.domain.model.SalesPerformance

interface SalesPerformanceRepository {
    // Null dateFrom/dateTo lets the backend default to the current month - see
    // .claude/docs/api-contract.md "Sales Performance". adminId: null means "see everyone" (only
    // valid for roles that carry that privilege - see RoleLevel.canSeeAllSalesPerformance());
    // passing the caller's own admin id scopes the response to just their own details.
    suspend fun get(dateFrom: String? = null, dateTo: String? = null, adminId: Int? = null): NetworkResult<SalesPerformance>
}
