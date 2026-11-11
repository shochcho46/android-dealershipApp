package com.example.dealernest.data.collection

import com.example.dealernest.core.network.ApiService
import com.example.dealernest.core.network.NetworkResult
import com.example.dealernest.core.network.PagedData
import com.example.dealernest.core.network.mapSuccess
import com.example.dealernest.core.network.safeApiCall
import com.example.dealernest.core.network.safePagedApiCall
import com.example.dealernest.data.collection.dto.CreateDsrCollectionRequestDto
import com.example.dealernest.data.collection.dto.toDomain
import com.example.dealernest.domain.model.DsrCollection
import com.example.dealernest.domain.repository.DsrCollectionFilters
import com.example.dealernest.domain.repository.DsrCollectionRepository
import java.io.IOException
import javax.inject.Inject

class DsrCollectionRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
) : DsrCollectionRepository {

    override suspend fun list(filters: DsrCollectionFilters, page: Int, perPage: Int) =
        safePagedApiCall {
            apiService.listDsrCollections(
                vendorId = filters.vendorId,
                dateFrom = filters.dateFrom,
                dateTo = filters.dateTo,
                page = page,
                perPage = perPage,
            )
        }.mapSuccess { paged -> PagedData(paged.items.map { it.toDomain() }, paged.meta) }

    override suspend fun create(
        vendorId: Int,
        amount: Double,
        collectionDate: String,
        note: String?,
    ): NetworkResult<DsrCollection> = safeApiCall {
        apiService.createDsrCollection(
            CreateDsrCollectionRequestDto(
                vendorId = vendorId,
                paymentMethodId = DEFAULT_PAYMENT_METHOD_ID,
                amount = amount,
                collectionDate = collectionDate,
                note = note,
            ),
        )
    }.mapSuccess { it.toDomain() }

    override suspend fun delete(id: Int): NetworkResult<Unit> = try {
        val response = apiService.deleteDsrCollection(id)
        if (response.isSuccessful) {
            NetworkResult.Success(Unit)
        } else {
            NetworkResult.Error(
                message = if (response.code() == 403) {
                    "You don't have permission to delete this collection."
                } else {
                    "Request failed (HTTP ${response.code()})"
                },
                isUnauthorized = response.code() == 401,
            )
        }
    } catch (e: IOException) {
        NetworkResult.Exception(e)
    }

    private companion object {
        // No payment-methods list endpoint exists in the API docs; the SRS itself says not to
        // show this on the UI and use a default - 1 matches the Postman/MD example (likely
        // "Cash"). See .claude/docs/api-contract.md.
        const val DEFAULT_PAYMENT_METHOD_ID = 1
    }
}
