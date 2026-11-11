package com.example.dealernest.data.product

import com.example.dealernest.core.database.dao.ProductDao
import com.example.dealernest.core.database.entity.toDomain
import com.example.dealernest.core.database.entity.toEntity
import com.example.dealernest.core.network.ApiService
import com.example.dealernest.core.network.NetworkResult
import com.example.dealernest.core.network.mapSuccess
import com.example.dealernest.core.network.safeApiCall
import com.example.dealernest.data.product.dto.toDomain
import com.example.dealernest.domain.model.Product
import com.example.dealernest.domain.repository.ProductRepository
import javax.inject.Inject

class ProductRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val productDao: ProductDao,
) : ProductRepository {

    // Read-through cache - see the comment on VendorRepositoryImpl.search() for why this isn't
    // a full offline-first dataset (no "list all products" endpoint exists).
    override suspend fun search(query: String, limit: Int): NetworkResult<List<Product>> {
        val result = safeApiCall { apiService.searchProducts(query.ifBlank { null }, limit) }
            .mapSuccess { list -> list.map { it.toDomain() } }
        return when (result) {
            is NetworkResult.Success -> {
                productDao.upsertAll(result.data.map { it.toEntity() })
                result
            }
            is NetworkResult.Exception -> {
                val cached = productDao.search(query, limit).map { it.toDomain() }
                if (cached.isNotEmpty()) NetworkResult.Success(cached, isFromCache = true) else result
            }
            is NetworkResult.Error -> result
        }
    }

    override suspend fun getById(id: Int): NetworkResult<Product> {
        val result = safeApiCall { apiService.getProduct(id) }.mapSuccess { it.toDomain() }
        return when (result) {
            is NetworkResult.Success -> {
                productDao.upsertAll(listOf(result.data.toEntity()))
                result
            }
            is NetworkResult.Exception -> {
                val cached = productDao.getById(id)?.toDomain()
                if (cached != null) NetworkResult.Success(cached, isFromCache = true) else result
            }
            is NetworkResult.Error -> result
        }
    }
}
