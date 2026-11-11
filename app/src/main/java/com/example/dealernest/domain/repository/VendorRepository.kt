package com.example.dealernest.domain.repository

import com.example.dealernest.core.location.LocationPoint
import com.example.dealernest.core.network.NetworkResult
import com.example.dealernest.domain.model.ImageUpload
import com.example.dealernest.domain.model.Vendor

data class VendorFormData(
    val shopName: String,
    val contactPerson: String,
    val email: String?,
    val mobile: String,
    val countryId: Int,
    val fullAddress: String,
    val status: Int = 1,
)

interface VendorRepository {
    suspend fun search(query: String, limit: Int): NetworkResult<List<Vendor>>
    suspend fun getById(id: Int): NetworkResult<Vendor>

    // lat/long come only from LocationPoint (GPS capture) - never a manual field, per the SRS.
    suspend fun create(form: VendorFormData, location: LocationPoint?, image: ImageUpload?): NetworkResult<Vendor>
    suspend fun update(id: Int, form: VendorFormData, location: LocationPoint?, image: ImageUpload?): NetworkResult<Vendor>
}
