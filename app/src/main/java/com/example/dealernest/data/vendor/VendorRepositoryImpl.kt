package com.example.dealernest.data.vendor

import com.example.dealernest.core.database.dao.VendorDao
import com.example.dealernest.core.database.entity.toDomain
import com.example.dealernest.core.database.entity.toEntity
import com.example.dealernest.core.location.LocationPoint
import com.example.dealernest.core.network.ApiService
import com.example.dealernest.core.network.NetworkResult
import com.example.dealernest.core.network.mapSuccess
import com.example.dealernest.core.network.safeApiCall
import com.example.dealernest.data.vendor.dto.toDomain
import com.example.dealernest.domain.model.ImageUpload
import com.example.dealernest.domain.model.Vendor
import com.example.dealernest.domain.repository.VendorFormData
import com.example.dealernest.domain.repository.VendorRepository
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject

class VendorRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val vendorDao: VendorDao,
) : VendorRepository {

    // Read-through cache: the API has no "list all vendors" endpoint, only search+details, so
    // this can't be a full offline-first dataset - it upserts whatever's been fetched and falls
    // back to those cached rows only when the network call fails outright (no connectivity).
    // See .claude/docs/roadmap.md Phase 5.
    override suspend fun search(query: String, limit: Int): NetworkResult<List<Vendor>> {
        val result = safeApiCall { apiService.searchVendors(query.ifBlank { null }, limit) }
            .mapSuccess { list -> list.map { it.toDomain() } }
        return when (result) {
            is NetworkResult.Success -> {
                vendorDao.upsertAll(result.data.map { it.toEntity() })
                result
            }
            is NetworkResult.Exception -> {
                val cached = vendorDao.search(query, limit).map { it.toDomain() }
                if (cached.isNotEmpty()) NetworkResult.Success(cached, isFromCache = true) else result
            }
            is NetworkResult.Error -> result
        }
    }

    override suspend fun getById(id: Int): NetworkResult<Vendor> {
        val result = safeApiCall { apiService.getVendor(id) }.mapSuccess { it.toDomain() }
        return when (result) {
            is NetworkResult.Success -> {
                vendorDao.upsertAll(listOf(result.data.toEntity()))
                result
            }
            is NetworkResult.Exception -> {
                val cached = vendorDao.getById(id)?.toDomain()
                if (cached != null) NetworkResult.Success(cached, isFromCache = true) else result
            }
            is NetworkResult.Error -> result
        }
    }

    override suspend fun create(
        form: VendorFormData,
        location: LocationPoint?,
        image: ImageUpload?,
    ): NetworkResult<Vendor> = safeApiCall {
        apiService.createVendor(
            shopName = form.shopName.asTextPart(),
            contactPerson = form.contactPerson.asTextPart(),
            email = form.email?.asTextPart(),
            mobile = form.mobile.asTextPart(),
            countryId = form.countryId.toString().asTextPart(),
            fullAddress = form.fullAddress.asTextPart(),
            lat = location?.latitude?.toString()?.asTextPart(),
            long = location?.longitude?.toString()?.asTextPart(),
            status = form.status.toString().asTextPart(),
            image = image?.toMultipartPart(),
        )
    }.mapSuccess { it.toDomain() }

    override suspend fun update(
        id: Int,
        form: VendorFormData,
        location: LocationPoint?,
        image: ImageUpload?,
    ): NetworkResult<Vendor> = safeApiCall {
        apiService.updateVendor(
            id = id,
            method = "PUT".asTextPart(),
            shopName = form.shopName.asTextPart(),
            contactPerson = form.contactPerson.asTextPart(),
            email = form.email?.asTextPart(),
            mobile = form.mobile.asTextPart(),
            countryId = form.countryId.toString().asTextPart(),
            fullAddress = form.fullAddress.asTextPart(),
            lat = location?.latitude?.toString()?.asTextPart(),
            long = location?.longitude?.toString()?.asTextPart(),
            status = form.status.toString().asTextPart(),
            image = image?.toMultipartPart(),
        )
    }.mapSuccess { it.toDomain() }
}

private fun String.asTextPart(): RequestBody = toRequestBody("text/plain".toMediaTypeOrNull())

private fun ImageUpload.toMultipartPart(): MultipartBody.Part {
    val body = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
    return MultipartBody.Part.createFormData("image", fileName, body)
}
