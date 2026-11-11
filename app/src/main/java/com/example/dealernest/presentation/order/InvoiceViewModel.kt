package com.example.dealernest.presentation.order

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dealernest.core.network.NetworkResult
import com.example.dealernest.domain.repository.OrderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InvoiceViewModel @Inject constructor(
    private val orderRepository: OrderRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<InvoiceUiState>(InvoiceUiState.Loading)
    val uiState: StateFlow<InvoiceUiState> = _uiState.asStateFlow()

    private var loadedOrderId: Int? = null

    fun load(orderId: Int) {
        if (loadedOrderId == orderId) return
        loadedOrderId = orderId
        fetch(orderId)
    }

    fun retry(orderId: Int) = fetch(orderId)

    private fun fetch(orderId: Int) {
        viewModelScope.launch {
            _uiState.value = InvoiceUiState.Loading
            when (val result = orderRepository.getById(orderId)) {
                is NetworkResult.Success -> _uiState.value = InvoiceUiState.Success(result.data)
                is NetworkResult.Error -> _uiState.value = InvoiceUiState.Error(result.message)
                is NetworkResult.Exception -> _uiState.value = InvoiceUiState.Error(
                    "Couldn't reach the server. Check your connection and try again.",
                )
            }
        }
    }
}
