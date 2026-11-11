package com.example.dealernest.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dealernest.core.common.daysAgoApiDateString
import com.example.dealernest.core.common.toApiDateString
import com.example.dealernest.core.common.todayApiDateString
import com.example.dealernest.core.common.toShortWeekdayLabel
import com.example.dealernest.core.network.NetworkResult
import com.example.dealernest.core.network.PagedData
import com.example.dealernest.core.session.SessionManager
import com.example.dealernest.core.session.SessionState
import com.example.dealernest.domain.model.AdminProfile
import com.example.dealernest.domain.model.DsrCollection
import com.example.dealernest.domain.model.Order
import com.example.dealernest.domain.model.SalesPerformance
import com.example.dealernest.domain.model.canSeeAllSalesPerformance
import com.example.dealernest.domain.repository.DsrCollectionFilters
import com.example.dealernest.domain.repository.DsrCollectionRepository
import com.example.dealernest.domain.repository.OrderFilters
import com.example.dealernest.domain.repository.OrderRepository
import com.example.dealernest.domain.repository.SalesPerformanceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val orderRepository: OrderRepository,
    private val collectionRepository: DsrCollectionRepository,
    private val salesPerformanceRepository: SalesPerformanceRepository,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow<DashboardUiState>(DashboardUiState.Loading)
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    // Own date range, independent of the rest of the dashboard - the API supports date_from/
    // date_to (defaults to current month when both are null). Changing this only re-fetches
    // Sales Performance, not the whole dashboard, so it doesn't flash Loading over everything.
    private val _salesPerformanceDateFromMillis = MutableStateFlow<Long?>(null)
    val salesPerformanceDateFromMillis: StateFlow<Long?> = _salesPerformanceDateFromMillis.asStateFlow()

    private val _salesPerformanceDateToMillis = MutableStateFlow<Long?>(null)
    val salesPerformanceDateToMillis: StateFlow<Long?> = _salesPerformanceDateToMillis.asStateFlow()

    private val _isSalesPerformanceRefreshing = MutableStateFlow(false)
    val isSalesPerformanceRefreshing: StateFlow<Boolean> = _isSalesPerformanceRefreshing.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch { load() }
    }

    fun onSalesPerformanceDateFromChange(millis: Long?) {
        _salesPerformanceDateFromMillis.value = millis
        refreshSalesPerformance()
    }

    fun onSalesPerformanceDateToChange(millis: Long?) {
        _salesPerformanceDateToMillis.value = millis
        refreshSalesPerformance()
    }

    private fun refreshSalesPerformance() {
        if (_uiState.value !is DashboardUiState.Success) return
        viewModelScope.launch {
            _isSalesPerformanceRefreshing.value = true
            val loggedIn = sessionManager.sessionState.first { it is SessionState.LoggedIn } as SessionState.LoggedIn
            val result = salesPerformanceRepository.get(
                dateFrom = _salesPerformanceDateFromMillis.value?.toApiDateString(),
                dateTo = _salesPerformanceDateToMillis.value?.toApiDateString(),
                adminId = loggedIn.profile.salesPerformanceAdminId(),
            )
            val updated = (result as? NetworkResult.Success)?.data
            (_uiState.value as? DashboardUiState.Success)?.let { current ->
                _uiState.value = current.copy(salesPerformance = updated)
            }
            _isSalesPerformanceRefreshing.value = false
        }
    }

    private suspend fun load() {
        _uiState.value = DashboardUiState.Loading
        val loggedIn = sessionManager.sessionState.first { it is SessionState.LoggedIn } as SessionState.LoggedIn
        val userId = loggedIn.profile.id
        val today = todayApiDateString()
        val weekAgo = daysAgoApiDateString(6)

        coroutineScope {
            val todayOrdersDeferred = async {
                orderRepository.listByPlacedBy(userId, OrderFilters(dateFrom = today, dateTo = today), 1, 100)
            }
            val weekOrdersDeferred = async {
                orderRepository.listByPlacedBy(userId, OrderFilters(dateFrom = weekAgo, dateTo = today), 1, 100)
            }
            val todayCollectionsDeferred = async {
                collectionRepository.list(DsrCollectionFilters(dateFrom = today, dateTo = today), 1, 100)
            }
            val weekCollectionsDeferred = async {
                collectionRepository.list(DsrCollectionFilters(dateFrom = weekAgo, dateTo = today), 1, 100)
            }
            val recentOrdersDeferred = async { orderRepository.listByPlacedBy(userId, OrderFilters(), 1, 5) }
            val recentCollectionsDeferred = async { collectionRepository.list(DsrCollectionFilters(), 1, 5) }
            // Called for every role now - Director/Admin/SuperAdmin/Subadmin omit admin_id and
            // see everyone; anyone else passes their own admin_id and the backend scopes the
            // response to just their own details. See .claude/docs/auth-and-permissions.md
            // "Role model". Respects whatever date range is already selected (e.g. a background
            // refresh() after this one wouldn't otherwise reset the user's chosen range).
            val salesPerformanceDeferred = async {
                salesPerformanceRepository.get(
                    dateFrom = _salesPerformanceDateFromMillis.value?.toApiDateString(),
                    dateTo = _salesPerformanceDateToMillis.value?.toApiDateString(),
                    adminId = loggedIn.profile.salesPerformanceAdminId(),
                )
            }

            val todayOrders = todayOrdersDeferred.await().itemsOrEmpty()
            val weekOrders = weekOrdersDeferred.await().itemsOrEmpty()
            val todayCollections = todayCollectionsDeferred.await().itemsOrEmpty()
            val weekCollections = weekCollectionsDeferred.await().itemsOrEmpty()
            val recentOrders = recentOrdersDeferred.await().itemsOrEmpty()
            val recentCollections = recentCollectionsDeferred.await().itemsOrEmpty()
            val salesPerformance: SalesPerformance? =
                (salesPerformanceDeferred.await() as? NetworkResult.Success)?.data

            val stats = DashboardStats(
                todayOrdersCount = todayOrders.size,
                todaySalesTotal = todayOrders.sumOf { it.totalAmount.toDoubleOrNull() ?: 0.0 },
                todayCollectionsTotal = todayCollections.sumOf { it.amount.toDoubleOrNull() ?: 0.0 },
                todayProductsSold = todayOrders.sumOf { order -> order.items.sumOf { it.quantity } },
            )

            val salesEntries = weekOrders.map { (it.createdAt?.take(10) ?: "") to (it.totalAmount.toDoubleOrNull() ?: 0.0) }
            val collectionEntries = weekCollections.map { it.collectionDate to (it.amount.toDoubleOrNull() ?: 0.0) }
            val chart = WeeklyChartData(
                salesByDay = bucketByDay(salesEntries),
                collectionsByDay = bucketByDay(collectionEntries),
            )

            _uiState.value = DashboardUiState.Success(stats, chart, recentOrders, recentCollections, salesPerformance)
        }
    }

    private fun bucketByDay(entries: List<Pair<String, Double>>): List<Pair<String, Double>> {
        val days = (0..6).map { daysAgoApiDateString(6 - it) }
        return days.map { day -> day.toShortWeekdayLabel() to entries.filter { it.first == day }.sumOf { it.second } }
    }

    private fun <T> NetworkResult<PagedData<T>>.itemsOrEmpty(): List<T> = (this as? NetworkResult.Success)?.data?.items.orEmpty()

    private fun AdminProfile.salesPerformanceAdminId(): Int? = if (roles.canSeeAllSalesPerformance()) null else id
}
