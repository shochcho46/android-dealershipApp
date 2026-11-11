package com.example.dealernest.presentation.vendor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dealernest.core.network.ConnectivityObserver
import com.example.dealernest.core.network.NetworkResult
import com.example.dealernest.core.qr.extractVendorSearchTerm
import com.example.dealernest.domain.repository.VendorRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VendorListViewModel @Inject constructor(
    private val vendorRepository: VendorRepository,
    connectivityObserver: ConnectivityObserver,
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _uiState = MutableStateFlow<VendorListUiState>(VendorListUiState.Loading)
    val uiState: StateFlow<VendorListUiState> = _uiState.asStateFlow()

    // The API has no page cursor for vendor search (search+limit only, capped at 50) - "load
    // more" raises this instead of paging. See .claude/docs/api-contract.md.
    private var limit = INITIAL_LIMIT
    private var searchJob: Job? = null
    private var isLoadingMore = false

    init {
        searchJob = viewModelScope.launch { performSearch() }

        // "Sync automatically when internet returns" (SRS) - refresh on a false->true
        // connectivity transition. See .claude/docs/roadmap.md Phase 5.
        viewModelScope.launch {
            var wasConnected: Boolean? = null
            connectivityObserver.isConnected.collect { connected ->
                if (wasConnected == false && connected) refresh()
                wasConnected = connected
            }
        }
    }

    fun onQueryChange(newQuery: String) {
        _query.value = newQuery
        limit = INITIAL_LIMIT
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            performSearch()
        }
    }

    fun onQrScanned(value: String) = onQueryChange(extractVendorSearchTerm(value))

    fun refresh() {
        limit = INITIAL_LIMIT
        searchJob?.cancel()
        searchJob = viewModelScope.launch { performSearch() }
    }

    fun loadMore() {
        if (isLoadingMore || limit >= MAX_LIMIT) return
        limit = (limit + PAGE_STEP).coerceAtMost(MAX_LIMIT)
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            isLoadingMore = true
            performSearch(isAppend = true)
            isLoadingMore = false
        }
    }

    private suspend fun performSearch(isAppend: Boolean = false) {
        if (!isAppend) _uiState.value = VendorListUiState.Loading
        when (val result = vendorRepository.search(_query.value, limit)) {
            is NetworkResult.Success -> {
                _uiState.value = if (result.data.isEmpty()) {
                    VendorListUiState.Empty
                } else {
                    VendorListUiState.Success(
                        vendors = result.data,
                        canLoadMore = limit < MAX_LIMIT && result.data.size >= limit,
                        isFromCache = result.isFromCache,
                    )
                }
            }
            is NetworkResult.Error -> _uiState.value = VendorListUiState.Error(result.message)
            is NetworkResult.Exception -> _uiState.value = VendorListUiState.Error(
                "Couldn't reach the server. Check your connection and try again.",
            )
        }
    }

    companion object {
        private const val INITIAL_LIMIT = 20
        private const val PAGE_STEP = 20
        private const val MAX_LIMIT = 50
        private const val SEARCH_DEBOUNCE_MS = 350L
    }
}
