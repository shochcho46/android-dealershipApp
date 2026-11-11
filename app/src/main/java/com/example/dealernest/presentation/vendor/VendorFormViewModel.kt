package com.example.dealernest.presentation.vendor

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dealernest.core.location.LocationProvider
import com.example.dealernest.core.network.NetworkResult
import com.example.dealernest.domain.model.ImageUpload
import com.example.dealernest.domain.repository.VendorFormData
import com.example.dealernest.domain.repository.VendorRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VendorFormViewModel @Inject constructor(
    private val vendorRepository: VendorRepository,
    private val locationProvider: LocationProvider,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow(VendorFormUiState())
    val uiState: StateFlow<VendorFormUiState> = _uiState.asStateFlow()

    private var editingVendorId: Int? = null

    fun loadForEdit(vendorId: Int) {
        if (editingVendorId == vendorId) return
        editingVendorId = vendorId
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingExisting = true) }
            when (val result = vendorRepository.getById(vendorId)) {
                is NetworkResult.Success -> {
                    val vendor = result.data
                    _uiState.update {
                        it.copy(
                            shopName = vendor.shopName,
                            contactPerson = vendor.contactPerson.orEmpty(),
                            email = vendor.email.orEmpty(),
                            mobile = vendor.mobile,
                            fullAddress = vendor.fullAddress.orEmpty(),
                            isActive = vendor.status,
                            existingImageUrl = vendor.imageUrl,
                            isLoadingExisting = false,
                        )
                    }
                }
                is NetworkResult.Error -> _uiState.update {
                    it.copy(isLoadingExisting = false, errorMessage = result.message)
                }
                is NetworkResult.Exception -> _uiState.update {
                    it.copy(isLoadingExisting = false, errorMessage = "Couldn't load this vendor.")
                }
            }
        }
    }

    fun onShopNameChange(value: String) = _uiState.update { it.copy(shopName = value, errorMessage = null) }
    fun onContactPersonChange(value: String) = _uiState.update { it.copy(contactPerson = value, errorMessage = null) }
    fun onEmailChange(value: String) = _uiState.update { it.copy(email = value, errorMessage = null) }
    fun onMobileChange(value: String) = _uiState.update { it.copy(mobile = value, errorMessage = null) }
    fun onFullAddressChange(value: String) = _uiState.update { it.copy(fullAddress = value, errorMessage = null) }
    fun onActiveChange(value: Boolean) = _uiState.update { it.copy(isActive = value) }
    fun onImagePicked(uri: Uri?) = _uiState.update { it.copy(imageUri = uri) }

    fun submit() {
        val state = _uiState.value
        if (state.shopName.isBlank() || state.contactPerson.isBlank() || state.mobile.isBlank() || state.fullAddress.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please fill in all required fields.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }

            // GPS capture, not manual entry - see .claude/docs/auth-and-permissions.md.
            val location = runCatching { locationProvider.getCurrentLocation() }.getOrNull()
            val image = state.imageUri?.let { readImageUpload(it) }
            val form = VendorFormData(
                shopName = state.shopName.trim(),
                contactPerson = state.contactPerson.trim(),
                email = state.email.trim().ifBlank { null },
                mobile = state.mobile.trim(),
                countryId = DEFAULT_COUNTRY_ID,
                fullAddress = state.fullAddress.trim(),
                status = if (state.isActive) 1 else 0,
            )
            val result = editingVendorId?.let { vendorRepository.update(it, form, location, image) }
                ?: vendorRepository.create(form, location, image)

            when (result) {
                is NetworkResult.Success -> _uiState.update { it.copy(isSaving = false, saved = true) }
                is NetworkResult.Error -> _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = result.errors?.values?.firstOrNull()?.firstOrNull() ?: result.message,
                    )
                }
                is NetworkResult.Exception -> _uiState.update {
                    it.copy(isSaving = false, errorMessage = "Couldn't reach the server. Check your connection and try again.")
                }
            }
        }
    }

    private fun readImageUpload(uri: Uri): ImageUpload? {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
        val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
        val extension = mimeType.substringAfterLast('/', "jpg")
        return ImageUpload(bytes = bytes, fileName = "vendor_${editingVendorId ?: "new"}.$extension", mimeType = mimeType)
    }

    private companion object {
        // No country-picker data source exists in the API docs (no list endpoint) - hardcoded,
        // not shown in the UI, per user instruction. See .claude/docs/api-contract.md.
        const val DEFAULT_COUNTRY_ID = 18
    }
}
