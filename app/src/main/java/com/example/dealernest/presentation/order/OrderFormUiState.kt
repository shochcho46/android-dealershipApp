package com.example.dealernest.presentation.order

import com.example.dealernest.domain.model.Product
import com.example.dealernest.domain.model.Vendor

data class OrderFormUiState(
    val vendor: OrderVendorSelection? = null,
    val vendorQuery: String = "",
    val vendorResults: List<Vendor> = emptyList(),
    val isSearchingVendor: Boolean = false,
    val canLoadMoreVendors: Boolean = false,
    val productQuery: String = "",
    val productResults: List<Product> = emptyList(),
    val productResultsFromCache: Boolean = false,
    val isSearchingProduct: Boolean = false,
    val canLoadMoreProducts: Boolean = false,
    val cartLines: List<OrderCartLine> = emptyList(),
    val isLoadingExisting: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val saved: Boolean = false,
    // True once an existing order is loaded whose status is locked (see Order.isEditable) -
    // the whole form renders read-only in that case.
    val isReadOnly: Boolean = false,
)
