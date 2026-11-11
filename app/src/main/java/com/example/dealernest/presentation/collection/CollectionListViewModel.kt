package com.example.dealernest.presentation.collection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dealernest.core.common.toApiDateString
import com.example.dealernest.core.network.NetworkResult
import com.example.dealernest.core.session.SessionManager
import com.example.dealernest.core.session.SessionState
import com.example.dealernest.domain.model.RoleLevel
import com.example.dealernest.domain.repository.DsrCollectionFilters
import com.example.dealernest.domain.repository.DsrCollectionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CollectionListViewModel @Inject constructor(
    private val collectionRepository: DsrCollectionRepository,
    sessionManager: SessionManager,
) : ViewModel() {

    val roleLevel: StateFlow<RoleLevel> = sessionManager.sessionState
        .map { (it as? SessionState.LoggedIn)?.profile?.roleLevel ?: RoleLevel.NORMAL }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RoleLevel.NORMAL)

    private val _dateFromMillis = MutableStateFlow<Long?>(null)
    val dateFromMillis: StateFlow<Long?> = _dateFromMillis.asStateFlow()

    private val _dateToMillis = MutableStateFlow<Long?>(null)
    val dateToMillis: StateFlow<Long?> = _dateToMillis.asStateFlow()

    private val _uiState = MutableStateFlow<CollectionListUiState>(CollectionListUiState.Loading)
    val uiState: StateFlow<CollectionListUiState> = _uiState.asStateFlow()

    private var page = 1
    private var isLoadingMore = false

    init {
        viewModelScope.launch { fetch() }
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
        if (isLoadingMore || state !is CollectionListUiState.Success || page >= state.lastPage) return
        page += 1
        viewModelScope.launch {
            isLoadingMore = true
            fetch(append = true)
            isLoadingMore = false
        }
    }

    fun deleteCollection(id: Int) {
        viewModelScope.launch {
            collectionRepository.delete(id)
            refresh()
        }
    }

    private suspend fun fetch(append: Boolean = false) {
        if (!append) _uiState.value = CollectionListUiState.Loading
        val filters = DsrCollectionFilters(
            dateFrom = _dateFromMillis.value?.toApiDateString(),
            dateTo = _dateToMillis.value?.toApiDateString(),
        )
        when (val result = collectionRepository.list(filters, page, PER_PAGE)) {
            is NetworkResult.Success -> {
                val paged = result.data
                val existing = (_uiState.value as? CollectionListUiState.Success)?.collections.orEmpty()
                val items = if (append) existing + paged.items else paged.items
                _uiState.value = if (items.isEmpty()) {
                    CollectionListUiState.Empty
                } else {
                    CollectionListUiState.Success(
                        collections = items,
                        page = paged.meta.currentPage,
                        lastPage = paged.meta.lastPage,
                        filteredTotalAmount = paged.meta.filteredTotalAmount,
                    )
                }
            }
            is NetworkResult.Error -> _uiState.value = CollectionListUiState.Error(result.message)
            is NetworkResult.Exception -> _uiState.value = CollectionListUiState.Error(
                "Couldn't reach the server. Check your connection and try again.",
            )
        }
    }

    private companion object {
        const val PER_PAGE = 15
    }
}
