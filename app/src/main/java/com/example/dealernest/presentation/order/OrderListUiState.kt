package com.example.dealernest.presentation.order

import com.example.dealernest.domain.model.Order

sealed interface OrderListUiState {
    data object Loading : OrderListUiState
    data class Success(val orders: List<Order>, val page: Int, val lastPage: Int) : OrderListUiState
    data object Empty : OrderListUiState
    data class Error(val message: String) : OrderListUiState
}
