package com.example.dealernest.presentation.vendor

import com.example.dealernest.domain.model.Vendor

sealed interface VendorDetailsUiState {
    data object Loading : VendorDetailsUiState
    data class Success(val vendor: Vendor) : VendorDetailsUiState
    data class Error(val message: String) : VendorDetailsUiState
}
