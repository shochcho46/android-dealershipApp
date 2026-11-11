package com.example.dealernest.presentation.vendor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dealernest.core.network.NetworkResult
import com.example.dealernest.domain.repository.VendorRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VendorDetailsViewModel @Inject constructor(
    private val vendorRepository: VendorRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<VendorDetailsUiState>(VendorDetailsUiState.Loading)
    val uiState: StateFlow<VendorDetailsUiState> = _uiState.asStateFlow()

    private var loadedVendorId: Int? = null

    fun load(vendorId: Int) {
        if (loadedVendorId == vendorId && _uiState.value is VendorDetailsUiState.Success) return
        loadedVendorId = vendorId
        fetch(vendorId)
    }

    fun retry(vendorId: Int) = fetch(vendorId)

    private fun fetch(vendorId: Int) {
        viewModelScope.launch {
            _uiState.value = VendorDetailsUiState.Loading
            when (val result = vendorRepository.getById(vendorId)) {
                is NetworkResult.Success -> _uiState.value = VendorDetailsUiState.Success(result.data)
                is NetworkResult.Error -> _uiState.value = VendorDetailsUiState.Error(result.message)
                is NetworkResult.Exception -> _uiState.value = VendorDetailsUiState.Error(
                    "Couldn't reach the server. Check your connection and try again.",
                )
            }
        }
    }
}
