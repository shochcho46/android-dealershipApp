package com.example.dealernest.data.auth

import com.example.dealernest.core.network.ApiService
import com.example.dealernest.core.network.NetworkResult
import com.example.dealernest.core.network.safeApiCall
import com.example.dealernest.core.session.SessionManager
import com.example.dealernest.data.auth.dto.LoginRequestDto
import com.example.dealernest.data.auth.dto.toDomain
import com.example.dealernest.domain.model.AdminProfile
import com.example.dealernest.domain.repository.AuthRepository
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val sessionManager: SessionManager,
) : AuthRepository {

    override suspend fun login(emailOrPhone: String, password: String): NetworkResult<AdminProfile> {
        val loginResult = safeApiCall {
            apiService.login(LoginRequestDto(emailOrPhone = emailOrPhone, password = password))
        }
        val loginResponse = when (loginResult) {
            is NetworkResult.Success -> loginResult.data
            is NetworkResult.Error -> return loginResult
            is NetworkResult.Exception -> return loginResult
        }

        // Token must be saved before any subsequent call - AuthInterceptor needs it. Login
        // confirmedly inlines the full admin object, so no separate profile call is needed.
        sessionManager.saveToken(loginResponse.token)
        val profile = loginResponse.admin.toDomain()
        sessionManager.saveProfile(profile)
        return NetworkResult.Success(profile)
    }

    override suspend fun refreshProfile(): NetworkResult<AdminProfile> {
        val result = safeApiCall { apiService.getProfile() }
        if (result is NetworkResult.Success) {
            sessionManager.saveProfile(result.data.toDomain())
        }
        return when (result) {
            is NetworkResult.Success -> NetworkResult.Success(result.data.toDomain())
            is NetworkResult.Error -> result
            is NetworkResult.Exception -> result
        }
    }

    override suspend fun logout() {
        // Best-effort - a dead network on logout shouldn't trap the user in a logged-in state.
        runCatching { apiService.logout() }
        sessionManager.clearSession()
    }
}
