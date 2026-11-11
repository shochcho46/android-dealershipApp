package com.example.dealernest.presentation.order

import com.example.dealernest.domain.model.Order

sealed interface InvoiceUiState {
    data object Loading : InvoiceUiState
    data class Success(val order: Order) : InvoiceUiState
    data class Error(val message: String) : InvoiceUiState
}
