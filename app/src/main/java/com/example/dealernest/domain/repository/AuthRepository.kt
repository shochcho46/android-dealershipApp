package com.example.dealernest.domain.repository

import com.example.dealernest.core.network.NetworkResult
import com.example.dealernest.domain.model.AdminProfile

interface AuthRepository {
    suspend fun login(emailOrPhone: String, password: String): NetworkResult<AdminProfile>
    suspend fun refreshProfile(): NetworkResult<AdminProfile>
    suspend fun logout()
}
