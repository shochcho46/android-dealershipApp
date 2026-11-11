package com.example.dealernest.presentation.collection

import com.example.dealernest.domain.model.Vendor

data class CollectionCreateUiState(
    val query: String = "",
    val vendorResults: List<Vendor> = emptyList(),
    val isSearching: Boolean = false,
    val canLoadMoreVendors: Boolean = false,
    val selectedVendor: Vendor? = null,
    val amount: String = "",
    val note: String = "",
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val saved: Boolean = false,
)
