package com.example.dealernest.presentation.vendor

import android.net.Uri

data class VendorFormUiState(
    val shopName: String = "",
    val contactPerson: String = "",
    val email: String = "",
    val mobile: String = "",
    val fullAddress: String = "",
    val isActive: Boolean = true,
    val imageUri: Uri? = null,
    val existingImageUrl: String? = null,
    val isLoadingExisting: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val saved: Boolean = false,
)
