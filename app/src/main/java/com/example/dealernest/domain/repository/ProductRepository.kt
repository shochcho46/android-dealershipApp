package com.example.dealernest.domain.repository

import com.example.dealernest.core.network.NetworkResult
import com.example.dealernest.domain.model.Product

interface ProductRepository {
    suspend fun search(query: String, limit: Int): NetworkResult<List<Product>>
    suspend fun getById(id: Int): NetworkResult<Product>
}
