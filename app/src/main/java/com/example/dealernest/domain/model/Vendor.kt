package com.example.dealernest.domain.model

data class Vendor(
    val id: Int,
    val shopName: String,
    val contactPerson: String?,
    val email: String?,
    val mobile: String,
    val countryName: String?,
    val fullAddress: String?,
    val status: Boolean,
    val statusText: String,
    val imageUrl: String?,
    val dueBalance: String?,
    val oldDue: String?,
    val totalCredit: String?,
    val totalDebit: String?,
    // Server-computed - "yes"/"no" for whether this vendor has GPS coordinates on file, and a
    // ready-to-use hex color for it. Null on backends/cache entries predating this field.
    val locationStatus: String? = null,
    val locationStatusColor: String? = null,
) {
    val hasLocationSet: Boolean get() = locationStatus.equals("yes", ignoreCase = true)
}
