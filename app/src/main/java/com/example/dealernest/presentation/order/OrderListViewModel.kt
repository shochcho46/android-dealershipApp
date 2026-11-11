package com.example.dealernest.presentation.order

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dealernest.core.common.toApiDateString
import com.example.dealernest.core.network.NetworkResult
import com.example.dealernest.core.session.SessionManager
import com.example.dealernest.core.session.SessionState
import com.example.dealernest.domain.repository.OrderFilters
import com.example.dealernest.domain.repository.OrderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

@HiltViewModel
class OrderListViewModel @Inject constructor(
    private val orderRepository: OrderRepository,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow<OrderListUiState>(OrderListUiState.Loading)
    val uiState: StateFlow<OrderListUiState> = _uiState.asStateFlow()

    private val _dateFromMillis = MutableStateFlow<Long?>(null)
    val dateFromMillis: StateFlow<Long?> = _dateFromMillis.asStateFlow()

    private val _dateToMillis = MutableStateFlow<Long?>(null)
    val dateToMillis: StateFlow<Long?> = _dateToMillis.asStateFlow()

    private var page = 1
    private var currentUserId: Int? = null
    private var isLoadingMore = false

    init {
        viewModelScope.launch {
            // Bounded, not sessionState.first {} unbounded - an unresolved/slow session read used
            // to leave this screen stuck on the Loading skeleton forever with no way out, since
            // fetch() (the only thing that ever replaces Loading) was gated behind it indefinitely.
            val loggedIn = withTimeoutOrNull(SESSION_WAIT_TIMEOUT_MS) {
                sessionManager.sessionState.first { it is SessionState.LoggedIn }
            } as? SessionState.LoggedIn
            if (loggedIn == null) {
                _uiState.value = OrderListUiState.Error("Couldn't verify your session. Please try again.")
                return@launch
            }
            currentUserId = loggedIn.profile.id
            fetch()
        }
    }

    fun onDateFromChange(millis: Long?) {
        _dateFromMillis.value = millis
        refresh()
    }

    fun onDateToChange(millis: Long?) {
        _dateToMillis.value = millis
        refresh()
    }

    fun refresh() {
        page = 1
        viewModelScope.launch { fetch() }
    }

    fun loadNextPage() {
        val state = _uiState.value
        if (isLoadingMore || state !is OrderListUiState.Success || page >= state.lastPage) return
        page += 1
        viewModelScope.launch {
            isLoadingMore = true
            fetch(append = true)
            isLoadingMore = false
        }
    }

    fun cancelOrder(id: Int) {
        viewModelScope.launch {
            orderRepository.cancel(id)
            refresh()
        }
    }

    private suspend fun fetch(append: Boolean = false) {
        val userId = currentUserId ?: return
        if (!append) _uiState.value = OrderListUiState.Loading
        val filters = OrderFilters(
            dateFrom = _dateFromMillis.value?.toApiDateString(),
            dateTo = _dateToMillis.value?.toApiDateString(),
        )
        when (val result = orderRepository.listByPlacedBy(userId, filters, page, PER_PAGE)) {
            is NetworkResult.Success -> {
                val paged = result.data
                val existing = (_uiState.value as? OrderListUiState.Success)?.orders.orEmpty()
                val items = if (append) existing + paged.items else paged.items
                _uiState.value = if (items.isEmpty()) {
                    OrderListUiState.Empty
                } else {
                    OrderListUiState.Success(items, paged.meta.currentPage, paged.meta.lastPage)
                }
            }
            is NetworkResult.Error -> _uiState.value = OrderListUiState.Error(result.message)
            is NetworkResult.Exception -> _uiState.value = OrderListUiState.Error(
                "Couldn't reach the server. Check your connection and try again.",
            )
        }
    }

    private companion object {
        const val PER_PAGE = 15
        const val SESSION_WAIT_TIMEOUT_MS = 8_000L
    }
}
