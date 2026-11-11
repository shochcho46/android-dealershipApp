package com.example.dealernest.presentation.collection

import com.example.dealernest.domain.model.DsrCollection

sealed interface CollectionListUiState {
    data object Loading : CollectionListUiState
    data class Success(
        val collections: List<DsrCollection>,
        val page: Int,
        val lastPage: Int,
        val filteredTotalAmount: String?,
    ) : CollectionListUiState
    data object Empty : CollectionListUiState
    data class Error(val message: String) : CollectionListUiState
}
