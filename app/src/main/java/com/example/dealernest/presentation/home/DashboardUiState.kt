package com.example.dealernest.presentation.home

import com.example.dealernest.domain.model.DsrCollection
import com.example.dealernest.domain.model.Order
import com.example.dealernest.domain.model.SalesPerformance

// "Total Vendors Visited" from the SRS is deliberately omitted - there's no visit-tracking
// endpoint anywhere in the API docs to derive it from. See .claude/docs/roadmap.md Phase 4.
data class DashboardStats(
    val todayOrdersCount: Int,
    val todaySalesTotal: Double,
    val todayCollectionsTotal: Double,
    val todayProductsSold: Int,
)

data class WeeklyChartData(
    val salesByDay: List<Pair<String, Double>>,
    val collectionsByDay: List<Pair<String, Double>>,
)

sealed interface DashboardUiState {
    data object Loading : DashboardUiState
    data class Success(
        val stats: DashboardStats,
        val chart: WeeklyChartData,
        val recentOrders: List<Order>,
        val recentCollections: List<DsrCollection>,
        // Null for Normal users (never fetched) or if the admin-only call failed - the rest of
        // the dashboard still renders either way, see DashboardViewModel.load().
        val salesPerformance: SalesPerformance? = null,
    ) : DashboardUiState
    data class Error(val message: String) : DashboardUiState
}
