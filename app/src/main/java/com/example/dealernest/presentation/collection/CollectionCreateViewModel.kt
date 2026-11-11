package com.example.dealernest.presentation.collection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dealernest.core.common.todayApiDateString
import com.example.dealernest.core.network.NetworkResult
import com.example.dealernest.core.qr.extractVendorSearchTerm
import com.example.dealernest.domain.model.Vendor
import com.example.dealernest.domain.repository.DsrCollectionRepository
import com.example.dealernest.domain.repository.VendorRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CollectionCreateViewModel @Inject constructor(
    private val collectionRepository: DsrCollectionRepository,
    private val vendorRepository: VendorRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CollectionCreateUiState())
    val uiState: StateFlow<CollectionCreateUiState> = _uiState.asStateFlow()

    // Same "search+limit only, capped at 50" API shape as the main Vendor tab - "load more"
    // raises this instead of paging. See VendorListViewModel / .claude/docs/api-contract.md.
    private var limit = INITIAL_LIMIT
    private var searchJob: Job? = null

    fun onQueryChange(value: String) {
        _uiState.update { it.copy(query = value) }
        limit = INITIAL_LIMIT
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300)
            search()
        }
    }

    fun onQrScanned(value: String) = onQueryChange(extractVendorSearchTerm(value))

    fun loadMoreVendors() {
        if (limit >= MAX_LIMIT) return
        limit = (limit + PAGE_STEP).coerceAtMost(MAX_LIMIT)
        searchJob?.cancel()
        searchJob = viewModelScope.launch { search() }
    }

    fun onVendorSelected(vendor: Vendor) = _uiState.update {
        it.copy(selectedVendor = vendor, vendorResults = emptyList(), query = "")
    }

    fun onChangeVendor() = _uiState.update { it.copy(selectedVendor = null) }
    fun onAmountChange(value: String) = _uiState.update { it.copy(amount = value, errorMessage = null) }
    fun onNoteChange(value: String) = _uiState.update { it.copy(note = value) }

    fun submit() {
        val state = _uiState.value
        val vendor = state.selectedVendor ?: return
        val amount = state.amount.toDoubleOrNull()
        if (amount == null || amount <= 0.0) {
            _uiState.update { it.copy(errorMessage = "Enter a valid amount.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            val result = collectionRepository.create(
                vendorId = vendor.id,
                amount = amount,
                collectionDate = todayApiDateString(),
                note = state.note.trim().ifBlank { null },
            )
            when (result) {
                is NetworkResult.Success -> _uiState.update { it.copy(isSaving = false, saved = true) }
                is NetworkResult.Error -> _uiState.update { it.copy(isSaving = false, errorMessage = result.message) }
                is NetworkResult.Exception -> _uiState.update {
                    it.copy(isSaving = false, errorMessage = "Couldn't reach the server. Check your connection and try again.")
                }
            }
        }
    }

    private suspend fun search() {
        val query = _uiState.value.query
        _uiState.update { it.copy(isSearching = true) }
        when (val result = vendorRepository.search(query, limit)) {
            is NetworkResult.Success -> _uiState.update {
                it.copy(
                    vendorResults = result.data,
                    isSearching = false,
                    canLoadMoreVendors = limit < MAX_LIMIT && result.data.size >= limit,
                    errorMessage = null,
                )
            }
            is NetworkResult.Error -> _uiState.update {
                it.copy(vendorResults = emptyList(), isSearching = false, canLoadMoreVendors = false, errorMessage = result.message)
            }
            is NetworkResult.Exception -> _uiState.update {
                it.copy(
                    vendorResults = emptyList(),
                    isSearching = false,
                    canLoadMoreVendors = false,
                    errorMessage = "Couldn't search vendors. Check your connection and try again.",
                )
            }
        }
    }

    private companion object {
        const val INITIAL_LIMIT = 20
        const val PAGE_STEP = 20
        const val MAX_LIMIT = 50
    }
}
