package com.example.dealernest.presentation.vendor

import com.example.dealernest.domain.model.Vendor

sealed interface VendorListUiState {
    data object Loading : VendorListUiState
    data class Success(
        val vendors: List<Vendor>,
        val canLoadMore: Boolean,
        val isFromCache: Boolean = false,
    ) : VendorListUiState
    data object Empty : VendorListUiState
    data class Error(val message: String) : VendorListUiState
}
